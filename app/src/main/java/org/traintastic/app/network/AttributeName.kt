package org.traintastic.app.network

enum class AttributeName(val value: UShort) {
    Visible(0u),
    Enabled(1u),
    Min(2u),
    Max(3u),
    Category(4u),
    ObjectEditor(5u),
    Values(6u),
    SubObject(7u),
    ClassList(8u),
    ObjectList(9u),
    DisplayName(10u),
    AliasKeys(11u),
    AliasValues(12u);

    companion object {
        fun fromUShort(value: UShort) = AttributeName.values().first { it.value == value }
    }
}