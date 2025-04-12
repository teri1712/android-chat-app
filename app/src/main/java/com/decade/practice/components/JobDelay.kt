package com.decade.practice.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface JobDelay {
    fun save(scope: CoroutineScope, block: suspend () -> Unit)
}

@Singleton
class NetworkJobDelay @Inject constructor(context: Context) : NetworkCallback(), JobDelay {

    private val jobList: MutableList<Pair<CoroutineScope, suspend () -> Unit>> = mutableListOf()
    private val mainScope = MainScope()
    private var connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var netAvailable = true

    override fun save(scope: CoroutineScope, block: suspend () -> Unit) {
        mainScope.launch {
            if (netAvailable) {
                scope.launch {
                    block()
                }
            } else {
                jobList.add(Pair(scope, block))
            }
        }
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        val cap = connManager.getNetworkCapabilities(network)
        if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            netAvailable = true
            jobList.forEach {
                it.first.launch {
                    it.second()
                }
            }
            jobList.clear()
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        netAvailable = false
    }
}