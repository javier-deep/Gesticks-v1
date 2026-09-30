package com.example.gesticks.wear

import android.content.Context
import android.util.Log
import com.example.gesticks.data.model.Ticket
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson

class WearSyncManager(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val gson = Gson()

    fun syncTickets(tickets: List<Ticket>) {
        val putDataMapReq = PutDataMapRequest.create("/tickets").apply {
            // Convertimos la lista a JSON para enviarla de forma sencilla
            val ticketsJson = gson.toJson(tickets)
            dataMap.putString("tickets_json", ticketsJson)
            // Añadimos un timestamp para forzar la actualización aunque los datos sean iguales
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d("WearSyncManager", "Tickets sincronizados con éxito")
            }
            .addOnFailureListener { e ->
                Log.e("WearSyncManager", "Error al sincronizar tickets", e)
            }
    }
}
