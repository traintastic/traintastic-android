package org.traintastic.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.traintastic.app.databinding.ServerListBinding


class ServerListFragment : Fragment() {

    private var _binding: ServerListBinding? = null
    private val serversListViewModel by viewModels<ServersListViewModel> {
        ServersListViewModelFactory(requireContext().applicationContext)
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = ServerListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val linearLayoutManager = LinearLayoutManager(requireContext().applicationContext)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvServers.layoutManager = linearLayoutManager

        val serversAdapter = ServersAdaptor { server -> adapterOnClick(server) }
        binding.rvServers.adapter = serversAdapter

        serversListViewModel.serversLiveData.observe(this, {
            it?.let {
                serversAdapter.submitList(it as MutableList<Server>)
            }
        })

        binding.fabAddServer.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun adapterOnClick(server: Server) {
        //val intent = Intent(this, FlowerDetailActivity()::class.java)
        //intent.putExtra(FLOWER_ID, flower.id)
        //startActivity(intent)
    }
}