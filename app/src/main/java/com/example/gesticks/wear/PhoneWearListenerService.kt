package com.example.gesticks.wear

import android.util.Log
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TICKET_STATUS_PATH = "/ticket_status"
private const val TAG = "PhoneWearListener"

/**
 * Recibe el mensaje que el reloj envía al elegir un nuevo estado para un ticket
 * ("$ticketId|$estado") y ejecuta la transición real contra el backend, ya que
 * el reloj no tiene acceso directo a la API (solo se comunica con el teléfono).
 */
class PhoneWearListenerService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != TICKET_STATUS_PATH) return

        val payload = String(messageEvent.data).split("|")
        val ticketId = payload.getOrNull(0)?.toIntOrNull() ?: return
        val targetStatus = payload.getOrNull(1) ?: return

        val sessionManager = SessionManager(applicationContext)
        val token = sessionManager.fetchAuthToken() ?: return
        val userId = sessionManager.fetchUserId()
        val repository = TicketRepository()

        scope.launch {
            try {
                val response = when (targetStatus) {
                    "abierto" -> repository.reopenTicket(token, ticketId)
                    "pendiente" -> repository.markTicketInProgress(token, ticketId)
                    "resuelto" -> repository.resolveTicket(token, ticketId)
                    else -> return@launch
                }
                if (!response.isSuccessful) {
                    Log.e(TAG, "No se pudo actualizar el ticket $ticketId: ${response.code()}")
                }

                val ticketsResponse = repository.getTickets(token)
                if (ticketsResponse.isSuccessful) {
                    val assignedTickets = ticketsResponse.body()?.data.orEmpty()
                        .filter { it.assignedToId == userId }
                    WearSyncManager(applicationContext).syncTickets(assignedTickets)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando ticket $ticketId desde el reloj", e)
            }
        }
    }
}
