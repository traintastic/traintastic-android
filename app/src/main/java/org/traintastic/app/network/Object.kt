package org.traintastic.app.network

class Object(val handle: UInt, val classId: String) {

    internal val interfaceItems: HashMap<String, InterfaceItem> = HashMap()

    fun getProperty(name: String): Property? {
        val item = interfaceItems.getOrDefault(name, null)
        return if (item is Property) item else null
    }

    fun getMethod(name: String): Method? {
        val item = interfaceItems.getOrDefault(name, null)
        return if (item is Method) item else null
    }
}