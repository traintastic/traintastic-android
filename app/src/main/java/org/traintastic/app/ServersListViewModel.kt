package org.traintastic.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.net.InetAddress

class ServersListViewModel(val dataSource: ServersDataSource) : ViewModel() {

    val serversLiveData = dataSource.getServerList()

    fun insertServer(ip: InetAddress?, port: Int?, hostname: String?, versionMajor: UInt?, versionMinor: UInt?, versionPatch: UInt?, persistent: Boolean) {
        if (ip == null || port == null) {
            return
        }

        val newServer = Server(
            ip,
            port,
            hostname ?: "${ip}:${port}",
            versionMajor ?: 0u,
            versionMinor ?: 0u,
            versionPatch ?: 0u,
            persistent ?: false
        )

        dataSource.addServer(newServer)
    }
}

class ServersListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServersListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServersListViewModel(
                dataSource = ServersDataSource.getDataSource(context.resources)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}