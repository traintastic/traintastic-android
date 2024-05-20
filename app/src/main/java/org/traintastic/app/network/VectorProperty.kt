package org.traintastic.app.network

class VectorProperty(obj: Object, name: String, val type: ValueType, val flags: Set<PropertyFlags>, private var values: Array<Any>, val enumOrSetName: String = "") : InterfaceItem(obj, name) {

    //private val onChanged: HashMap<UInt, ((property :VectorProperty)->Unit)> = HashMap()

    fun getBool(index: Int) : Boolean {
        assert(type == ValueType.Boolean)
        return if (values[index] is Boolean) values[index] as Boolean else false
    }

    fun getInt(index: Int) : Int {
        assert(getLong(index) >= Int.MIN_VALUE && getLong(index) <= Int.MAX_VALUE)
        return getLong(index).toInt()
    }

    fun getLong(index: Int) : Long {
        assert(type == ValueType.Integer)
        return if (values[index] is Long) values[index] as Long else 0
    }

    fun getDouble(index: Int) : Double {
        assert(type == ValueType.Float)
        return if (values[index] is Double) values[index] as Double else Double.NaN
    }

    fun getString(index: Int): String {
        assert(type == ValueType.String)
        return if (values[index] is String) values[index] as String else ""
    }

    internal fun updateValue(newValues: Array<Any>) {
        values = newValues
        //onChanged.forEach { (_, callback) -> callback.invoke(this) }
    }

    /*companion object {
        private var onChangedHandle: UInt = 0u

        private fun getHandle(): UInt {
            return if (onChangedHandle < UInt.MAX_VALUE) ++onChangedHandle else 1u
        }
    }*/
}