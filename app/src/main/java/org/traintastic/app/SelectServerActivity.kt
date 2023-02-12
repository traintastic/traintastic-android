package org.traintastic.app

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import org.traintastic.app.databinding.ActivityMainBinding
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.timerTask


class SelectServerActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: DatagramSocket
    private lateinit var discoveryPacket: DatagramPacket
    private lateinit var timer: Timer
    private lateinit var broadcastTask: TimerTask
    private lateinit var udpReceiveThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        socket = DatagramSocket()
        socket.broadcast = true

        val data = Message.request(Message.Command.Discover).toByteArray()
        discoveryPacket = DatagramPacket(data, data.size, getBroadcastAddress(), DEFAULT_PORT)

        timer = Timer()
    }

    override fun onResume() {
        super.onResume()

        // Start sending and receiving
        broadcastTask = timerTask {
            socket.send(discoveryPacket)
        }
        timer.scheduleAtFixedRate(broadcastTask,0,1000)

        udpReceiveThread = Thread(Runnable {
            receiveUDP()
        })
        udpReceiveThread.start()
    }

    override fun onPause() {
        // Stop sending and receiving
        broadcastTask.cancel()
        timer.purge()
        udpReceiveThread.interrupt()

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                this.startActivity(Intent(this,SettingsActivity::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    //@Throws(IOException::class)
    private fun getBroadcastAddress(): InetAddress? {
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8).toByte()
        return InetAddress.getByAddress(quads)
    }

    private fun receiveUDP() {
        val buffer = ByteArray(1500)
        try {
            val packet = DatagramPacket(buffer, buffer.size)
            while (true) {
                // TODO: is it safe to use socket in this thread?
                socket.receive(packet)

                val msg = Message.fromByteArray(packet.data.sliceArray(IntRange(0, packet.length - 1)))
                if (msg.getCommand() == Message.Command.Discover && msg.getType() == Message.Type.Response) {
                    val serverName = msg.readString()
                    val versionMajor = msg.readUInt16()
                    val versionMinor = msg.readUInt16()
                    val versionPatch = msg.readUInt16()

                    //Log.d("udp_receive", "serverName = " + serverName)
                    //Log.d("udp_receive", "versionMajor = " + versionMajor)
                    //Log.d("udp_receive", "versionMinor = " + versionMinor)
                    //Log.d("udp_receive", "versionPatch = " + versionPatch)

                    val servers = ServersDataSource.getDataSource(applicationContext.resources)
                    if (!servers.hasServer(packet.address, packet.port)) {
                        servers.addServer(
                            Server(
                                packet.address,
                                packet.port,
                                serverName,
                                versionMajor,
                                versionMinor,
                                versionPatch,
                                false
                            )
                        )
                    }
                }

                //Log.d("udp_receive", packet.data.toString())
                //Log.d("udp_receive", packet.length.toString())
            }
        } catch (e: Exception) {
            Log.d("udp_receive", "Exception: " + e.toString())
            e.printStackTrace()
        } finally {
            //socket?.close()
        }
    }
}
