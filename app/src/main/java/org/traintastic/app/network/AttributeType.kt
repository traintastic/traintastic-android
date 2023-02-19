package org.traintastic.app.network

enum class AttributeType(val value: UByte) {
    Value(1u),
    Values(2u);

    companion object {
        fun fromByte(value: Byte) = AttributeType.values().first { it.value == value.toUByte() }
    }
}