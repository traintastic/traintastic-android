package org.traintastic.app

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.InetAddress

class ServersDataSource(resources: Resources) {
    private val initialServerList = serverList(resources)
    private val serversLiveData = MutableLiveData(initialServerList)

    fun addServer(server: Server) {
        val currentList = serversLiveData.value
        if (currentList == null) {
            serversLiveData.postValue(listOf(server))
        } else {
            val updatedList = currentList.toMutableList()
            updatedList.add(0, server)
            serversLiveData.postValue(updatedList)
        }
    }

    fun removeServer(server: Server) {
        val currentList = serversLiveData.value
        if (currentList != null) {
            val updatedList = currentList.toMutableList()
            updatedList.remove(server)
            serversLiveData.postValue(updatedList)
        }
    }

    fun hasServer(ip: InetAddress, port: Int): Boolean {
        serversLiveData.value?.let { servers ->
            return servers.any{ it.ip == ip && it.port == port }
        }
        return false
    }

    fun getServer(ip: InetAddress, port: Int): Server? {
        serversLiveData.value?.let { servers ->
            return servers.firstOrNull{ it.ip == ip && it.port == port }
        }
        return null
    }

    fun getServerList(): LiveData<List<Server>> {
        return serversLiveData
    }

    companion object {
        private var INSTANCE: ServersDataSource? = null

        fun getDataSource(resources: Resources): ServersDataSource {
            return synchronized(ServersDataSource::class) {
                val newInstance = INSTANCE ?: ServersDataSource(resources)
                INSTANCE = newInstance
                newInstance
            }
        }
    }
}