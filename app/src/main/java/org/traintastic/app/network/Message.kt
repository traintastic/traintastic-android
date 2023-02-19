package org.traintastic.app.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

class Message private constructor() {
    // TODO: use ByteBuffer for data?

    enum class Command(val value: UByte) {
        Invalid(0u),
        Ping(1u),
        Login(2u),
        NewSession(3u),
        ServerLog(5u),
        ImportWorld(9u),
        ExportWorld(10u),
        CreateObject(11u),
        GetObject(14u),
        ReleaseObject(15u),
        ObjectSetProperty(16u),
        ObjectPropertyChanged(17u),
        ObjectAttributeChanged(18u),
        GetTableModel(19u),
        ReleaseTableModel(20u),
        TableModelColumnHeadersChanged(21u),
        TableModelRowCountChanged(22u),
        TableModelSetRegion(23u),
        TableModelUpdateRegion(24u),
        ObjectCallMethod(25u),
        ObjectSetUnitPropertyUnit(26u),
        ObjectSetObjectPropertyById(27u),
        ObjectDestroyed(28u),
        InputMonitorGetInputInfo(30u),
        InputMonitorInputIdChanged(31u),
        InputMonitorInputValueChanged(32u),
        OutputKeyboardGetOutputInfo(33u),
        OutputKeyboardSetOutputValue(34u),
        OutputKeyboardOutputIdChanged(35u),
        OutputKeyboardOutputValueChanged(36u),
        BoardGetTileData(37u),
        BoardTileDataChanged(38u),
        OutputMapGetItems(39u),
        OutputMapGetOutputs(40u),
        OutputMapOutputsChanged(41u),
        ObjectEventFired(42u),
        BoardGetTileInfo(43u),
        ObjectGetObjectPropertyObject(44u),
        Discover(255u);

        fun toByte(): Byte {
            return value.toByte()
        }

        companion object {
            fun fromByte(value: Byte) = values().first { it.value == value.toUByte() }
        }
    }

    enum class Type(val value: UByte) {
        Request(0x40u),
        Response(0x80u),
        Event(0xC0u);

        fun toByte(): Byte {
            return value.toByte()
        }

        companion object {
            fun fromByte(value: Byte) = values().first { it.value == value.toUByte() }
        }
    }

    enum class ErrorCode(val value: UByte) {
        NoError(0u),
        InvalidCommand(1u),
        Failed(2u),
        AuthenticationFailed(3u),
        InvalidSession(4u),
        UnknownObject(5u),
        ObjectNotTable(6u),
        UnknownClassId(7u);

        companion object {
            fun fromByte(value: Byte) = ErrorCode.values().first { it.value == value.toUByte() }
        }
    }

    private lateinit var data: ByteArray
    private var readPosition: Int = headerSize
    private val block: Stack<Int> = Stack()

    fun toByteArray(): ByteArray {
        setDataSize(data.size - headerSize)
        return data
    }

    fun getCommand(): Command {
        return Command.fromByte(data[0])
    }

    private fun setCommand(value: Command) {
        data[0] = value.toByte()
    }

    fun getType(): Type {
        return Type.fromByte(data[1] and 0xC0u.toByte())
    }

    private fun setType(value: Type) {
        data[1] = value.toByte() or (data[1] and 0x3F)
    }

    fun isError(): Boolean {
        return getErrorCode() != ErrorCode.NoError
    }

    fun getErrorCode(): ErrorCode {
        return ErrorCode.fromByte(data[1] and 0x3F)
    }

    fun getRequestId(): UShort {
        return (data[2].toUByte().toUInt() + (data[3].toUByte().toUInt() shl 8)).toUShort()
    }

    private fun setRequestId(value: UInt) {
        data[2] = (value and 0xFFu).toByte()
        data[3] = (value shr 8).toByte()
    }

    private fun setDataSize(value: Int) {
        data[4] = (value and 0xFF).toByte()
        data[5] = ((value shr 8) and 0xFF).toByte()
        data[6] = ((value shr 16) and 0xFF).toByte()
        data[7] = (value shr 24).toByte()
    }

    fun readBool(): Boolean {
        return readByte() != 0.toByte()
    }

    fun readByte(): Byte {
        val value = data[readPosition]
        readPosition += 1
        return value
    }

    fun readUInt16(): UShort {
        val value = data[readPosition].toUByte().toUInt() + (data[readPosition + 1].toUByte().toUInt() shl 8)
        readPosition += 2
        return value.toUShort()
    }

    fun writeInt32(value: Int) {
        data += (value and 0xFF).toByte()
        data += ((value shr 8) and 0xFF).toByte()
        data += ((value shr 16) and 0xFF).toByte()
        data += (value shr 24).toByte()
    }

    fun readInt32(): Int {
        val value = data[readPosition].toUByte().toInt() + (data[readPosition + 1].toUByte().toInt() shl 8) + (data[readPosition + 2].toUByte().toInt() shl 16) + (data[readPosition + 3].toUByte().toInt() shl 24)
        readPosition += 4
        return value
    }

    fun readUInt32(): UInt {
        val value = data[readPosition].toUByte().toUInt() + (data[readPosition + 1].toUByte().toUInt() shl 8) + (data[readPosition + 2].toUByte().toUInt() shl 16) + (data[readPosition + 3].toUByte().toUInt() shl 24)
        readPosition += 4
        return value
    }

    fun writeUInt32(value: UInt) {
        writeInt32(value.toInt())
    }

    fun readInt64(): Long {
        val bb = ByteBuffer.wrap(data, readPosition, 8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        val value = bb.getLong(0)
        readPosition += 8
        return value
    }

    fun readDouble(): Double {
        val bb = ByteBuffer.wrap(data, readPosition, 8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        val value = bb.getDouble(0)
        readPosition += 8
        return value
    }

    fun readString(): String {
        val length = readLength()
        val value = String(data.sliceArray(IntRange(readPosition, readPosition + length - 1)))
        readPosition += length
        return value
    }

    fun writeString(value: String) {
        val bytes = value.toByteArray()
        writeLength(bytes.size)
        if (bytes.isNotEmpty()) {
            data += bytes
        }
    }

    fun readUUID(): UUID {
        val bb = ByteBuffer.wrap(data, readPosition, 16)
        bb.order(ByteOrder.BIG_ENDIAN)
        val high: Long = bb.long
        val low: Long = bb.long
        readPosition += 16
        return UUID(high, low)
    }


    fun readBlock(): Int {
        val blockSize = readInt32()
        assert(blockSize >= 0)
        block.push(readPosition + blockSize)
        return blockSize
    }

    fun readBlockEnd() {
        assert(!block.empty())
        assert(readPosition <= block.peek())
        readPosition = block.pop()
    }

    fun endOfBlock(): Boolean {
        assert(!block.empty())
        return readPosition == block.peek()
    }

    private fun readLength(): Int {
        return readUInt32().toInt() // convert it to Int, as most params are Int in Kotlin
    }

    private fun writeLength(value: Int) {
        writeInt32(value)
    }

    companion object {
        const val headerSize = 8

        private var lastRequestId: UInt = 0u

        fun fromByteArray(array: ByteArray): Message {
            val m = Message()
            m.data = array
            return m
        }

        fun request(command: Command): Message {
            val m = Message()
            m.data = ByteArray(headerSize)
            m.setCommand(command)
            m.setType(Type.Request)
            m.setRequestId(nextRequestId())
            return m
        }

        fun response(command: Command, requestId: UInt): Message {
            val m = Message()
            m.data = ByteArray(headerSize)
            m.setCommand(command)
            m.setType(Type.Response)
            m.setRequestId(requestId)
            return m
        }

        fun event(command: Command): Message {
            val m = Message()
            m.data = ByteArray(headerSize)
            m.setCommand(command)
            m.setType(Type.Event)
            return m
        }

        private fun nextRequestId(): UInt {
            return if (lastRequestId < 65535u) {
                ++lastRequestId
            } else {
                lastRequestId = 0u
                lastRequestId
            }
        }
    }
}