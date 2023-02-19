package org.traintastic.app.network

enum class ValueType(val value: UByte) {
    Invalid(0u),
    Boolean(1u),
    Enum(2u),
    Integer(3u),
    Float(4u),
    String(5u),
    Object(6u),
    Set(7u);

    companion object {
        fun fromByte(value: Byte) = ValueType.values().first { it.value == value.toUByte() }
    }
}