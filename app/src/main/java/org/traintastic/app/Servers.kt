package org.traintastic.app

import android.content.res.Resources
import java.net.InetAddress

fun serverList(resources: Resources): List<Server> {
    return listOf(
        Server(
            ip = InetAddress.getByName("10.0.2.2"),
            port = 5740,
            hostname = "Android host system",
            versionMajor = 0u,
            versionMinor = 0u,
            versionPatch = 0u,
            persistent = true
        )
    )
}