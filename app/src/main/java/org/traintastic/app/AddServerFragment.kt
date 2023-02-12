package org.traintastic.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import org.traintastic.app.databinding.AddServerBinding
import java.net.InetAddress

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AddServerFragment : Fragment() {

    private var _binding: AddServerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = AddServerBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editPort.setText(DEFAULT_PORT.toString())

        binding.buttonSecond.setOnClickListener {
            val name = binding.editName.text.toString()
            val ip: InetAddress? = try { InetAddress.getByName(binding.editIPAddress.text.toString()) } catch (e: Exception) { null }
            val port = binding.editPort.text.toString().toIntOrNull() ?: 0

            if (name.isEmpty()) {
                Toast.makeText(requireContext().applicationContext, R.string.name_cant_be_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ip == null)
            {
                Toast.makeText(requireContext().applicationContext, R.string.invalid_ip_address, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (port < PORT_MIN || port > PORT_MAX)
            {
                Toast.makeText(requireContext().applicationContext, resources.getString(R.string.invalid_port_number_valid_range_x_x, PORT_MIN, PORT_MAX), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val servers = ServersDataSource.getDataSource(requireContext().applicationContext.resources)
            if (!servers.hasServer(ip, port)) {
                servers.addServer(
                    Server(
                        ip,
                        port,
                        name,
                        persistent = true
                    )
                )

                findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}