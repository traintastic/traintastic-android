package org.traintastic.app

import java.net.InetAddress

data class Server(
    val ip: InetAddress,
    val port: Int = DEFAULT_PORT,
    val hostname: String,
    val versionMajor: UInt = 0u,
    val versionMinor: UInt = 0u,
    val versionPatch: UInt = 0u,
    val persistent: Boolean
)