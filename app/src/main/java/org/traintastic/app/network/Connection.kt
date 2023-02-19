package org.traintastic.app.network

import android.os.Handler
import android.os.Looper
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue

object Connection {

    private var socket: Socket? = null
    private var readerThread: Thread? = null
    private var writerThread: Thread? = null
    private val writeQueue: ArrayBlockingQueue<Message> = ArrayBlockingQueue(64)
    private var objects: HashMap<UInt, Object> = HashMap()
    private val requests: HashMap<UShort, ((response: Message)->Unit)> = HashMap()
    private val onWorldChanged: HashMap<UInt, (()->Unit)> = HashMap()
    var onConnected: (()->Unit)? = null
    var traintastic: Object? = null
    var world: Object? = null

    fun enable(s: Socket) {
        socket = s

        readerThread = Thread {
            try {
                // NOTE: Don't know if this is safe and "good", but it works :)
                val stream = socket!!.getInputStream()
                var buffer = ByteArray(64 * 1024)
                while (true) {
                    stream.read(buffer, 0, Message.headerSize)
                    val dataSize = buffer[4].toUByte().toInt() + (buffer[5].toUByte()
                        .toInt() shl 8) + (buffer[6].toUByte()
                        .toInt() shl 16) + (buffer[7].toUByte()
                        .toInt() shl 24)
                    val size = Message.headerSize + dataSize
                    if (dataSize > 0) {
                        if (size > buffer.size) {
                            buffer = buffer.copyOf(size)
                        }
                        stream.read(buffer, Message.headerSize, dataSize)
                    }
                    val message = Message.fromByteArray(buffer.sliceArray(IntRange(0, size - 1)))

                    Handler(Looper.getMainLooper()).post {
                        processMessage(message)
                    }
                }
            } catch (e: Exception) {
                e.message
            }
        }
        readerThread!!.start()

        writerThread = Thread {
            try {
                // NOTE: Don't know if this is safe and "good", but it works :)
                val stream = socket!!.getOutputStream()
                while (true) {
                    val message = writeQueue.take()
                    if (message != null) {
                        stream.write(message.toByteArray())
                    }
                }
            } catch (e: Exception) {
                e.message
            }
        }
        writerThread!!.start()

        // 1. send login
        val message = Message.request(Message.Command.Login)
        message.writeString("") // username
        message.writeString("") // password
        request(message) { loginResponse ->
            assert(loginResponse.getCommand() == Message.Command.Login)
            if (!loginResponse.isError()) {
                // 2. start new session
                request(Message.request(Message.Command.NewSession)) { newSessionResponse ->
                    assert(newSessionResponse.getCommand() == Message.Command.NewSession)
                    if (!newSessionResponse.isError()) {
                        newSessionResponse.readUUID() // currently unused
                        traintastic = readObject(newSessionResponse)
                        val worldProperty = traintastic?.getProperty("world")
                        worldProperty?.addOnChanged { _ -> getWorld() }
                        getWorld()
                        onConnected?.invoke()
                    }
                }
            }
        }
    }

    fun disable() {
        world = null
        traintastic = null
        writerThread?.interrupt()
        readerThread?.interrupt()
        socket?.close()
    }

    fun send(message: Message): Int {
        writeQueue.put(message)
        return message.getRequestId().toInt()
    }

    fun request(message: Message, callback: ((response: Message)->Unit)) {
        assert(message.getType() == Message.Type.Request)
        requests[message.getRequestId()] = callback
        writeQueue.put(message)
    }

    private fun processMessage(message: Message) {
        when (message.getType()) {
            Message.Type.Response -> {
                requests[message.getRequestId()]?.invoke(message)
                requests.remove(message.getRequestId())
            }
            Message.Type.Event -> {
                when (message.getCommand()) {
                    Message.Command.ObjectPropertyChanged -> {
                        val obj = objects[message.readUInt32()] ?: return

                        val name = message.readString()
                        val valueType = ValueType.fromByte(message.readByte())

                        val property = obj.getProperty(name)
                        if (property is Property) {
                            property.updateValue(readValue(message, valueType))
                        }
                    }
                    else -> {
                    }
                }
            }
            Message.Type.Request -> {

            }
        }
    }

    internal fun readObject(message: Message): Object {
        message.readBlock() // object

        val handle = message.readUInt32()
        var obj = objects[handle] // try get object by handle

        if (obj == null) {
            val classId = message.readString()
            obj = Object(handle, classId)

            objects[handle] = obj

            message.readBlock() // items
            while (!message.endOfBlock()) {
                message.readBlock() // item
                var item: InterfaceItem? = null
                val name = message.readString()

                when (val type = InterfaceItemType.fromByte(message.readByte())) {
                    InterfaceItemType.Property,
                    InterfaceItemType.UnitProperty,
                    InterfaceItemType.VectorProperty -> {
                        val flags = readPropertyFlags(message)
                        val valueType = ValueType.fromByte(message.readByte())
                        val enumOrSetName = if (valueType == ValueType.Enum || valueType == ValueType.Set) message.readString() else ""

                        if (type == InterfaceItemType.VectorProperty) {
                            //val length = message.readInt32() // read UInt as Int, Kotlin uses Int for length
                            assert(false) // TODO: implement VectorProperty
                        } else {
                            assert(type == InterfaceItemType.Property || type == InterfaceItemType.UnitProperty)

                            val value = readValue(message, valueType)

                            if (type == InterfaceItemType.UnitProperty) {
                                assert(false) // TODO: implement UnitProperty
                            } else {
                                item = Property(obj, name, valueType, flags, value, enumOrSetName)
                            }
                        }
                    }
                    InterfaceItemType.Method -> {
                        val resultType = ValueType.fromByte(message.readByte())
                        val argumentCount = message.readByte().toInt()
                        val argumentTypes =
                            Array(argumentCount) { ValueType.fromByte(message.readByte()) }
                        item = Method(obj, name, resultType, argumentTypes)
                    }
                    InterfaceItemType.Event -> {
                        val argumentCount = message.readByte().toInt()
                        val argumentTypes = Array(argumentCount) {
                                val v = ValueType.fromByte(message.readByte())
                                if (v == ValueType.Enum || v == ValueType.Set)
                                    message.readString() // enum/set type, currently unused
                                v
                            }
                        item = Event(obj, name, argumentTypes)
                    }
                }

                if (item != null) {
                    message.readBlock() // attributes
                    while (!message.endOfBlock()) {
                        message.readBlock() // item

                        val attributeName = AttributeName.fromUShort(message.readUInt16())
                        val valueType = ValueType.fromByte(message.readByte())
                        assert(valueType != ValueType.Object && valueType != ValueType.Invalid)

                        when (AttributeType.fromByte(message.readByte())) {
                            AttributeType.Value ->
                                item.attributes[attributeName] = readValue(message, valueType)
                            AttributeType.Values ->
                                item.attributes[attributeName] = Array(message.readInt32()) { readValue(message, valueType) }
                        }
                        
                        message.readBlockEnd() // end attribute
                    }
                    message.readBlockEnd() // end attributes
                }
                obj.interfaceItems[item!!.name] = item
                message.readBlockEnd() // end item
            }
            message.readBlockEnd() // end items
        }
        message.readBlockEnd() // end object

        return obj
    }

    private fun readPropertyFlags(message: Message): Set<PropertyFlags> {
        val mask = message.readUInt16().toUInt() // use UInt, UShort doesn't have shr
        val value = mutableSetOf<PropertyFlags>()

        when (mask and 0x03u) {
            1u -> { value.add(PropertyFlags.Read) }
            3u -> { value.add(PropertyFlags.ReadWrite) }
            0u,
            2u -> { assert(false) }
        }

        when ((mask shr 2) and 0x03u) {
            0u,
            1u -> { value.add(PropertyFlags.NoStore) }
            2u -> { value.add(PropertyFlags.Store) }
            3u -> { value.add(PropertyFlags.StoreState) }
        }

        if ((mask and 0x10u) != 0u) {
            value.add(PropertyFlags.SubObject)
        }

        when ((mask shr 5) and 0x03u) {
            0u,
            1u -> { value.add(PropertyFlags.NoScript) }
            2u -> { value.add(PropertyFlags.ScriptReadOnly) }
            3u -> { value.add(PropertyFlags.ScriptReadWrite) }
        }

        if ((mask and 0x80u) != 0u) {
            value.add(PropertyFlags.Internal)
        }

        return value.toSet()
    }

    private fun readValue(message: Message, valueType: ValueType): Any {
        return when (valueType) {
            ValueType.Boolean -> message.readBool()
            ValueType.Enum,
            ValueType.Integer,
            ValueType.Set -> message.readInt64()
            ValueType.Float -> message.readDouble()
            ValueType.String,
            ValueType.Object -> message.readString()
            else -> {
                assert(false)
                false
            }
        }
    }

    private var onWorldChangedHandle = 0u

    fun addOnWorldChanged(callback: (()->Unit)): UInt {
        val handle = if (onWorldChangedHandle < UInt.MAX_VALUE) ++onWorldChangedHandle else 1u
        onWorldChanged[handle] = callback
        return handle
    }

    fun removeOnWorldChanged(handle: UInt) {
        onWorldChanged.remove(handle)
    }

    private fun getWorld() {
        val worldProperty = traintastic?.getProperty("world")
        if (worldProperty?.hasObject() == true) {
            worldProperty.getObject { obj, ec ->
                if(ec == Message.ErrorCode.NoError) {
                    world = obj
                    worldChanged()
                }
            }
        } else {
            world = null
            worldChanged()
        }
    }

    private fun worldChanged() {
        onWorldChanged.forEach { (_, callback) -> callback.invoke() }
    }
}