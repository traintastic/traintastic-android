package org.traintastic.app.network

enum class InterfaceItemType(val value: UByte) {
    Property(1u),
    Method(2u),
    UnitProperty(3u),
    VectorProperty(4u),
    Event(5u);

    companion object {
        fun fromByte(value: Byte) = InterfaceItemType.values().first { it.value == value.toUByte() }
    }
}