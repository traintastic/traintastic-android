package org.traintastic.app.ui.main

//import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.traintastic.app.R
import org.traintastic.app.network.Connection

class InfoFragment : Fragment() {
    private var view: View? = null
    private var worldChangedHandle: UInt = 0u

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_info, container, false)

        // Traintastic:
        val version = Connection.traintastic?.getProperty("version")
        if (version != null) {
            val tv: TextView = view!!.findViewById(R.id.tvInfoTraintasticVersionValue)
            tv.text = version.asString()
        }

        // World
        worldChangedHandle = Connection.addOnWorldChanged() { worldChanged() }
        worldChanged()

        return view
    }

    override fun onDestroyView() {
        view = null
        Connection.removeOnWorldChanged(worldChangedHandle)

        super.onDestroyView()
    }

    private fun worldChanged() {
        //if (view == null || Connection.world == null) { return }

        (view?.findViewById(R.id.tvInfoWorldNameValue) as TextView).text = Connection.world?.getProperty("name")?.asString()
        (view?.findViewById(R.id.tvInfoWorldUUIDValue) as TextView).text = Connection.world?.getProperty("uuid")?.asString()
    }

    companion object {
        @JvmStatic
        fun newInstance() = InfoFragment()
    }
}