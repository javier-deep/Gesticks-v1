package com.example.gesticks.data.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gesticks.data.model.Notification
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.time.Instant

/**
 * Igual que la web (socket.io): el backend emite 'nueva-notificacion' como
 * broadcast global, así que aquí se filtra por receptor_id del usuario actual.
 */
object NotificationSocketManager {
    private const val TAG = "NotificationSocket"
    private const val EVENT_NEW_NOTIFICATION = "nueva-notificacion"

    private var socket: Socket? = null
    private var connectedUserId: Int = -1

    private val _newNotification = MutableLiveData<Notification>()
    val newNotification: LiveData<Notification> = _newNotification

    fun connect(userId: Int) {
        if (socket?.connected() == true && connectedUserId == userId) return
        disconnect()

        connectedUserId = userId
        try {
            socket = IO.socket(RetrofitInstance.SOCKET_URL).also { socket ->
                socket.on(EVENT_NEW_NOTIFICATION) { args -> handleEvent(args, userId) }
                socket.on(Socket.EVENT_CONNECT_ERROR) { Log.w(TAG, "No se pudo conectar al socket de notificaciones") }
                socket.connect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error iniciando socket de notificaciones", e)
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        connectedUserId = -1
    }

    private fun handleEvent(args: Array<Any>, userId: Int) {
        val json = args.getOrNull(0) as? JSONObject ?: return
        val receiverId = json.optInt("receptor_id", -1)
        if (receiverId != userId) return

        val notification = Notification(
            id = json.optString("id"),
            tipo = json.optString("tipo"),
            titulo = json.optString("titulo"),
            mensaje = json.optString("mensaje"),
            receiverId = receiverId,
            senderId = json.optInt("emisor_id", -1).takeIf { it != -1 },
            ticketId = json.optInt("ticket_id", -1).takeIf { it != -1 },
            leida = false,
            createdAt = json.optString("created_at").ifBlank { Instant.now().toString() }
        )
        _newNotification.postValue(notification)
    }
}
