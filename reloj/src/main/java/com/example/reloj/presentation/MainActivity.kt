package com.example.reloj.presentation

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.reloj.presentation.theme.GesticksTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var _ticketsState = mutableStateOf<List<WatchTicket>>(emptyList())
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GesticksWatchApp(ticketsState = _ticketsState)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/tickets") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val json = dataMap.getString("tickets_json")
                if (json != null) {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val rawTickets: List<Map<String, Any>> = gson.fromJson(json, type)

                    val mappedTickets = rawTickets.map { raw ->
                        WatchTicket(
                            id = (raw["id"] as? Double)?.toInt()?.toString() ?: "0",
                            title = raw["titulo"] as? String ?: "Sin título",
                            priority = raw["prioridad"] as? String ?: "Baja",
                            status = (raw["estado"] as? String)?.lowercase() ?: "abierto"
                        )
                    }
                    _ticketsState.value = mappedTickets
                }
            }
        }
    }
}

data class WatchTicket(
    val id: String,
    val title: String,
    val priority: String,
    val status: String = "abierto" // abierto | pendiente | resuelto
) {
    val isResolved: Boolean get() = status == "resuelto"
}

@Composable
fun GesticksWatchApp(ticketsState: MutableState<List<WatchTicket>>) {
    GesticksTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            val scope = rememberCoroutineScope()
            val view = LocalView.current

            var tickets by ticketsState

            ScreenScaffold(scrollState = listState) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Gestix",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            CircularProgressIndicator(
                                progress = { (tickets.count { it.isResolved }.toFloat() / tickets.size.toFloat()) },
                                modifier = Modifier.size(40.dp).padding(4.dp),
                                colors = ProgressIndicatorDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Pendientes: ${tickets.count { !it.isResolved }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    items(tickets.size) { index ->
                        val ticket = tickets[index]

                        // Animación de desaparición
                        AnimatedVisibility(
                            visible = !ticket.isResolved,
                            exit = shrinkVertically(animationSpec = tween(500)) + fadeOut() + slideOutHorizontally()
                        ) {
                            TicketCard(
                                ticket = ticket,
                                transformationSpec = transformationSpec,
                                onStatusChange = { targetStatus ->
                                    triggerHapticFeedback(view.context, ticket.priority)

                                    // El reloj no tiene acceso directo a la API: le pedimos
                                    // al teléfono que aplique el cambio de estado en el backend real.
                                    sendStatusChangeMessage(view.context, ticket.id, targetStatus)

                                    scope.launch {
                                        delay(if (targetStatus == "resuelto") 500 else 300)
                                        tickets = tickets.toMutableList().also {
                                            it[index] = it[index].copy(status = targetStatus)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sendStatusChangeMessage(context: Context, ticketId: String, targetStatus: String) {
    val payload = "$ticketId|$targetStatus"
    val messageClient = Wearable.getMessageClient(context)
    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, "/ticket_status", payload.toByteArray())
        }
    }
}

fun triggerHapticFeedback(context: Context, priority: String) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (vibrator.hasVibrator()) {
        when (priority) {
            "Crítica" -> {
                // Doble pulso fuerte
                val pattern = longArrayOf(0, 100, 50, 100)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
            "Alta" -> {
                // Pulso largo
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            else -> {
                // Pulso corto
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}

@Composable
fun TransformingLazyColumnItemScope.TicketCard(
    ticket: WatchTicket,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec,
    onStatusChange: (String) -> Unit
) {
    Card(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${ticket.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                // Punto de color de prioridad
                val dotColor = priorityColor(ticket.priority)
                androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = dotColor)
                }
            }
            Text(
                text = ticket.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = ticket.priority,
                style = MaterialTheme.typography.bodySmall,
                color = priorityColor(ticket.priority)
            )

            // Selector de estado: igual que en la web, se puede elegir libremente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusPill(
                    label = "Abierto",
                    color = MaterialTheme.colorScheme.error,
                    isCurrent = ticket.status == "abierto",
                    onClick = { onStatusChange("abierto") },
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Pendiente",
                    color = MaterialTheme.colorScheme.primary,
                    isCurrent = ticket.status == "pendiente",
                    onClick = { onStatusChange("pendiente") },
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Resuelto",
                    color = MaterialTheme.colorScheme.secondary,
                    isCurrent = ticket.status == "resuelto",
                    onClick = { onStatusChange("resuelto") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (isCurrent) Modifier.background(color) else Modifier.border(1.dp, color, RoundedCornerShape(50)))
            .then(if (isCurrent) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun priorityColor(priority: String): Color = when (priority) {
    "Crítica" -> MaterialTheme.colorScheme.error
    "Alta" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.secondary
}

@WearPreviewDevices
@Composable
fun WatchPreview() {
    val dummyTickets = remember {
        mutableStateOf(listOf(
            WatchTicket("1", "Error Servidor", "Crítica"),
            WatchTicket("2", "Ajuste UI", "Media")
        ))
    }
    GesticksWatchApp(ticketsState = dummyTickets)
}
