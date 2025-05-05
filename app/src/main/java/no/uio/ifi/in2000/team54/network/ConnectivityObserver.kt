package no.uio.ifi.in2000.team54.network

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {

    val isConnected: Flow<Boolean>
}