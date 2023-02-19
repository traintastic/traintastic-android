package org.traintastic.app.network

enum class PropertyFlags {
    Read,
    ReadWrite,

    NoStore,
    Store,
    StoreState,

    SubObject,

    NoScript,
    ScriptReadOnly,
    ScriptReadWrite,

    Internal,
}