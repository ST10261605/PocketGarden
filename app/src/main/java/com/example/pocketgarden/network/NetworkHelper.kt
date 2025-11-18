package com.example.pocketgarden.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkHelper(private val context: Context) {

    private val _networkStatus = MutableStateFlow(false)
    val networkStatus: StateFlow<Boolean> = _networkStatus

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    //check if device is currently online
    fun isOnline(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkNetworkConnectivityModern()
        } else {
            checkNetworkConnectivityLegacy()
        }
    }

    //modern way to check connectivity (api 23+)
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkNetworkConnectivityModern(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        return networkCapabilities?.let { nc ->
            when {
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } ?: false
    }


    // Legacy way to check connectivity (Pre-API 23)

    @Suppress("DEPRECATION")
    private fun checkNetworkConnectivityLegacy(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnectedOrConnecting == true
    }


     //Start monitoring network connectivity changes

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkStatus.value = true
            }

            override fun onLost(network: Network) {
                _networkStatus.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                _networkStatus.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Set initial state
        _networkStatus.value = isOnline()
    }


    //Check if device has internet access (not just connectivity)
    suspend fun hasInternetAccess(): Boolean {
        // relying on network capabilities
        return isOnline()
    }


    //Get detailed network type

    fun getNetworkType(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            capabilities?.let { nc ->
                when {
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
                    else -> "UNKNOWN"
                }
            } ?: "DISCONNECTED"
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.typeName ?: "DISCONNECTED"
        }
    }
}