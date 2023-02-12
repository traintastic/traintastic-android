package org.traintastic.app

import kotlin.experimental.and
import kotlin.experimental.or

class Message private constructor() {

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

    private val headerSize = 8

    private lateinit var data: ByteArray
    private var offset: Int = headerSize

    fun toByteArray(): ByteArray {
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
        return getErrorCode() > 0u
    }

    fun getErrorCode(): UByte {
        return (data[1] and 0x3F).toUByte()
    }

    fun getRequestId(): UInt {
        return data[2].toUInt() + (data[3].toUInt() shl 8)
    }

    private fun setRequestId(value: UInt) {
        data[2] = (value and 0xFFu).toByte()
        data[3] = (value shr 8).toByte()
    }

    fun readUInt16(): UInt {
        val value = data[offset] + (data[offset + 1].toInt() shl 8)
        offset += 2
        return value.toUInt()
    }

    fun readUInt32(): UInt {
        val value = data[offset] + (data[offset + 1].toInt() shl 8) + (data[offset + 2].toInt() shl 16) + (data[offset + 3].toInt() shl 24)
        offset += 4
        return value.toUInt()
    }

    fun readString(): String {
        val length = readLength()
        val value = String(data.sliceArray(IntRange(offset, offset + length - 1)))
        offset += length
        return value
    }

    private fun readLength(): Int {
        return readUInt32().toInt() // convert it to Int, as most params are Int in Kotlin
    }

    companion object {
        private var lastRequestId: UInt = 0u

        fun fromByteArray(array: ByteArray): Message {
            val m = Message()
            m.data = array
            return m
        }

        fun request(command: Command): Message {
            val m = Message()
            m.data = ByteArray(m.headerSize)
            m.setCommand(command)
            m.setType(Type.Request)
            m.setRequestId(nextRequestId())
            return m
        }

        fun response(command: Command, requestId: UInt): Message {
            val m = Message()
            m.data = ByteArray(m.headerSize)
            m.setCommand(command)
            m.setType(Type.Response)
            m.setRequestId(requestId)
            return m
        }

        fun event(command: Command): Message {
            val m = Message()
            m.data = ByteArray(m.headerSize)
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