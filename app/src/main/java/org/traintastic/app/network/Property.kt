package org.traintastic.app.network

class Property(obj: Object, name: String, val type: ValueType, val flags: Set<PropertyFlags>, private var value: Any, val enumOrSetName: String = "") : InterfaceItem(obj, name) {

    private val onChanged: HashMap<UInt, ((property :Property)->Unit)> = HashMap()

    fun asBool() : Boolean {
        assert(type == ValueType.Boolean)
        return if (value is Boolean) value as Boolean else false
    }

    fun asInt() : Int {
        assert(asLong() >= Int.MIN_VALUE && asLong() <= Int.MAX_VALUE)
        return asLong().toInt()
    }

    fun asLong() : Long {
        assert(type == ValueType.Integer)
        return if (value is Long) value as Long else 0
    }

    fun asDouble() : Double {
        assert(type == ValueType.Float)
        return if (value is Double) value as Double else Double.NaN
    }

    fun asString(): String {
        assert(type == ValueType.String)
        return if (value is String) value as String else ""
    }

    fun hasObject(): Boolean {
        assert(type == ValueType.Object)
        return (value is String) && (value as String).isNotEmpty()
    }

    fun getObject(callback: ((obj: Object?, ec: Message.ErrorCode)->Unit)) {
        val request = Message.request(Message.Command.ObjectGetObjectPropertyObject)
        request.writeUInt32(obj.handle)
        request.writeString(name)
        Connection.request(request) { response ->
            assert(response.getCommand() == Message.Command.ObjectGetObjectPropertyObject)
            callback.invoke(
                if (!response.isError()) Connection.readObject(response) else null,
                response.getErrorCode()
            )
        }
    }

    fun addOnChanged(callback: ((property :Property)->Unit)): UInt {
        val handle = getHandle()
        onChanged[handle] = callback
        return handle
    }

    fun removeOnChanged(handle: UInt) {
        onChanged.remove(handle)
    }

    internal fun updateValue(newValue: Any) {
        value = newValue
        onChanged.forEach { (_, callback) -> callback.invoke(this) }
    }

    companion object {
        private var onChangedHandle: UInt = 0u

        private fun getHandle(): UInt {
            return if (onChangedHandle < UInt.MAX_VALUE) ++onChangedHandle else 1u
        }
    }
}