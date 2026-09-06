package com.flydrop.app.data.nearby

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.flydrop.app.data.PickedFile
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class NearbyPeer(
    val endpointId: String,
    val name: String,
    val connected: Boolean = false,
)

data class IncomingConnection(
    val endpointId: String,
    val name: String,
    /** Both phones show this short code; users must compare it before accepting. */
    val authenticationToken: String,
)

data class NearbyConnectionsState(
    val active: Boolean = false,
    val peers: List<NearbyPeer> = emptyList(),
    val incomingConnection: IncomingConnection? = null,
    val message: String? = null,
)

/**
 * Real, local FlyDrop discovery and file transfer over Google Nearby Connections.
 *
 * The transport is intentionally separate from Android Quick Share: it advertises
 * only FlyDrop endpoints, requires both parties to accept the same authentication
 * token, and copies received files into Downloads/FlyDrop where Android permits it.
 */
class NearbyConnectionsRepository(private val context: Context) {

    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(NearbyConnectionsState())
    val state: StateFlow<NearbyConnectionsState> = _state.asStateFlow()

    private val pendingSends = ConcurrentHashMap<String, List<PickedFile>>()
    private val receivedPayloads = ConcurrentHashMap<Long, File>()
    private val receivedMetadata = ConcurrentHashMap<Long, FileMetadata>()
    private val completedPayloadIds = ConcurrentHashMap.newKeySet<Long>()
    private val outgoingPayloads = ConcurrentHashMap<Long, File>()
    private var advertisedEndpointName = "FlyDrop"

    fun start(localName: String) {
        if (_state.value.active) return
        _state.update { it.copy(message = null) }
        advertisedEndpointName = localName.ifBlank { "FlyDrop" }.take(MAX_ENDPOINT_NAME)

        val advertising = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        val discovery = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        client.startAdvertising(advertisedEndpointName, SERVICE_ID, connectionCallback, advertising)
            .addOnFailureListener(::reportTaskFailure)
        client.startDiscovery(SERVICE_ID, discoveryCallback, discovery)
            .addOnSuccessListener { _state.update { it.copy(active = true, message = "Looking for FlyDrop devices nearby…") } }
            .addOnFailureListener(::reportTaskFailure)
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        pendingSends.clear()
        outgoingPayloads.values.forEach(File::delete)
        outgoingPayloads.clear()
        _state.value = NearbyConnectionsState()
    }

    fun requestConnection(peer: NearbyPeer, files: List<PickedFile>) {
        if (files.isEmpty()) {
            _state.update { it.copy(message = "Choose at least one file before sending.") }
            return
        }
        pendingSends[peer.endpointId] = files
        client.requestConnection(advertisedEndpointName, peer.endpointId, connectionCallback)
            .addOnFailureListener {
                pendingSends.remove(peer.endpointId)
                reportTaskFailure(it)
            }
    }

    fun acceptIncomingConnection() {
        val incoming = _state.value.incomingConnection ?: return
        client.acceptConnection(incoming.endpointId, payloadCallback)
            .addOnSuccessListener { _state.update { it.copy(incomingConnection = null) } }
            .addOnFailureListener(::reportTaskFailure)
    }

    fun rejectIncomingConnection() {
        val incoming = _state.value.incomingConnection ?: return
        client.rejectConnection(incoming.endpointId)
        _state.update { it.copy(incomingConnection = null) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun permissionsDenied() = _state.update {
        it.copy(message = "Allow Nearby Wi-Fi and Bluetooth permissions to find FlyDrop devices.")
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _state.update { state ->
                if (state.peers.any { it.endpointId == endpointId }) state else {
                    state.copy(peers = state.peers + NearbyPeer(endpointId, info.endpointName))
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _state.update { state -> state.copy(peers = state.peers.filterNot { it.endpointId == endpointId }) }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _state.update {
                it.copy(
                    incomingConnection = IncomingConnection(endpointId, info.endpointName, info.authenticationToken),
                )
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                _state.update { state ->
                    state.copy(peers = state.peers.map { peer ->
                        if (peer.endpointId == endpointId) peer.copy(connected = true) else peer
                    }, message = "Connected. Transferring securely…")
                }
                pendingSends.remove(endpointId)?.let { files ->
                    scope.launch { sendFiles(endpointId, files) }
                }
            } else {
                pendingSends.remove(endpointId)
                _state.update { it.copy(message = "The nearby connection was not accepted.") }
            }
        }

        override fun onDisconnected(endpointId: String) {
            pendingSends.remove(endpointId)
            _state.update { state ->
                state.copy(
                    peers = state.peers.map { peer ->
                        if (peer.endpointId == endpointId) peer.copy(connected = false) else peer
                    },
                )
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> parseMetadata(payload.asBytes())
                Payload.Type.FILE -> payload.asFile()?.asJavaFile()?.let { file ->
                    receivedPayloads[payload.id] = file
                    deliverIfReady(payload.id)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    outgoingPayloads.remove(update.payloadId)?.delete()
                    completedPayloadIds += update.payloadId
                    deliverIfReady(update.payloadId)
                }
                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED,
                -> {
                    outgoingPayloads.remove(update.payloadId)?.delete()
                    _state.update { it.copy(message = "A nearby file transfer did not finish.") }
                }
                else -> Unit
            }
        }
    }

    private suspend fun sendFiles(endpointId: String, files: List<PickedFile>) {
        files.forEach { picked ->
            val source = copyIntoTransferCache(picked) ?: run {
                _state.update { it.copy(message = "Could not read ${picked.name}.") }
                return@forEach
            }
            val filePayload = Payload.fromFile(source)
            outgoingPayloads[filePayload.id] = source
            val metadata = JSONObject()
                .put("id", filePayload.id)
                .put("name", picked.name)
                .put("mime", picked.mimeType)
                .toString()
                .encodeToByteArray()
            client.sendPayload(endpointId, Payload.fromBytes(metadata))
            client.sendPayload(endpointId, filePayload)
        }
        _state.update { it.copy(message = "Sending files securely…") }
    }

    private fun parseMetadata(bytes: ByteArray?) {
        val value = bytes?.decodeToString() ?: return
        runCatching {
            JSONObject(value).let { json ->
                FileMetadata(json.getLong("id"), json.getString("name"), json.getString("mime"))
            }
        }.onSuccess { metadata ->
            receivedMetadata[metadata.payloadId] = metadata
            deliverIfReady(metadata.payloadId)
        }
    }

    private fun deliverIfReady(payloadId: Long) {
        if (payloadId !in completedPayloadIds) return
        val source = receivedPayloads.remove(payloadId) ?: return
        val metadata = receivedMetadata.remove(payloadId) ?: run {
            receivedPayloads[payloadId] = source
            return
        }
        completedPayloadIds.remove(payloadId)
        scope.launch {
            val destination = saveReceivedFile(source, metadata)
            source.delete()
            _state.update {
                it.copy(message = if (destination != null) {
                    "Received ${metadata.name} in Downloads/FlyDrop."
                } else {
                    "Received ${metadata.name}, but FlyDrop could not save it."
                })
            }
        }
    }

    private fun copyIntoTransferCache(picked: PickedFile): File? = runCatching {
        val directory = File(context.cacheDir, "nearby-outgoing").apply { mkdirs() }
        val target = File(directory, "${System.nanoTime()}-${picked.name.safeFileName()}")
        context.contentResolver.openInputStream(picked.uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()

    private fun saveReceivedFile(source: File, metadata: FileMetadata): Uri? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, metadata.name.safeFileName())
                put(MediaStore.Downloads.MIME_TYPE, metadata.mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/FlyDrop")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: return null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val folder = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?.resolve("FlyDrop")?.apply { mkdirs() } ?: return null
            val target = File(folder, metadata.name.safeFileName())
            source.copyTo(target, overwrite = true)
            Uri.fromFile(target)
        }
    }.getOrNull()

    private fun reportTaskFailure(error: Exception) {
        val detail = (error as? ApiException)?.statusCode
        _state.update { it.copy(active = false, message = "Nearby sharing could not start${detail?.let { code -> " (error $code)" }.orEmpty()}.") }
    }

    private data class FileMetadata(val payloadId: Long, val name: String, val mimeType: String)

    private companion object {
        const val SERVICE_ID = "com.flydrop.app"
        const val MAX_ENDPOINT_NAME = 131
        val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    }
}

private fun String.safeFileName(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
