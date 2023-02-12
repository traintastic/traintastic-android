package org.traintastic.app

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ServersAdaptor(private val onClick: (Server) -> Unit) :
    ListAdapter<Server, ServersAdaptor.ServerViewHolder>(ServerDiffCallback) {

    class ServerViewHolder(itemView: View, val onClick: (Server) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val tvHostname: TextView = itemView.findViewById(R.id.tvHostname)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvVersion: TextView = itemView.findViewById(R.id.tvVersion)
        private var currentServer: Server? = null

        init {
            itemView.setOnClickListener {
                currentServer?.let {
                    onClick(it)
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(server: Server) {
            currentServer = server

            tvHostname.text = server.hostname

            if (server.port != DEFAULT_PORT) {
                tvAddress.text = "${server.ip.hostAddress}:${server.port}"
            } else {
                tvAddress.text = server.ip.hostAddress
            }

            if (server.versionMajor > 0u || server.versionMinor > 0u || server.versionPatch > 0u) {
                tvVersion.text = "v${server.versionMajor}.${server.versionMinor}.${server.versionPatch}"
            } else {
                tvVersion.text = ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_list_item, parent, false)
        return ServerViewHolder(view, onClick)
    }

    /* Gets current server and uses it to bind view. */
    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = getItem(position)
        holder.bind(server)

    }
}

object ServerDiffCallback : DiffUtil.ItemCallback<Server>() {
    override fun areItemsTheSame(oldItem: Server, newItem: Server): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Server, newItem: Server): Boolean {
        return oldItem.ip == newItem.ip && oldItem.port == newItem.port
    }
}
