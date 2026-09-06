package com.flydrop.app.ui.nearby

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.PickedFile
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.nearby.NearbyConnectionsRepository
import com.flydrop.app.data.nearby.NearbyConnectionsState
import com.flydrop.app.data.nearby.NearbyPeer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/** Lifecycle owner for the direct FlyDrop nearby transport. */
class NearbyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NearbyConnectionsRepository(application)
    val state: StateFlow<NearbyConnectionsState> = repository.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NearbyConnectionsState(),
    )

    private var localName = "FlyDrop"

    fun bind(user: FlyUser?) {
        localName = user?.let { "${it.name} · ${it.flyId}" } ?: "FlyDrop guest"
    }

    fun start() = repository.start(localName)
    fun stop() = repository.stop()
    fun send(peer: NearbyPeer, files: List<PickedFile>) = repository.requestConnection(peer, files)
    fun acceptIncomingConnection() = repository.acceptIncomingConnection()
    fun rejectIncomingConnection() = repository.rejectIncomingConnection()
    fun clearMessage() = repository.clearMessage()
    fun permissionsDenied() = repository.permissionsDenied()

    override fun onCleared() {
        repository.close()
    }
}
