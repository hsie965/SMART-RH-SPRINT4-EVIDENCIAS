package com.smartrh.wearos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.smartrh.wearos.data.ApiConfig
import com.smartrh.wearos.data.WearableRepository
import com.smartrh.wearos.model.LocationPayload
import com.smartrh.wearos.sensors.DeviceInfoProvider
import com.smartrh.wearos.sensors.LocationProvider
import com.smartrh.wearos.sensors.SensorMonitor
import com.smartrh.wearos.ui.theme.SmartRhWearTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var sensorMonitor: SensorMonitor
    private lateinit var deviceInfoProvider: DeviceInfoProvider
    private lateinit var locationProvider: LocationProvider
    private val repository = WearableRepository()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorMonitor = SensorMonitor(this)
        deviceInfoProvider = DeviceInfoProvider(this)
        locationProvider = LocationProvider(this)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BODY_SENSORS
            )
        )

        sensorMonitor.start()

        setContent {
            SmartRhWearTheme {
                SmartRhWearApp(
                    repository = repository,
                    sensorMonitor = sensorMonitor,
                    deviceInfoProvider = deviceInfoProvider,
                    locationProvider = locationProvider
                )
            }
        }
    }

    override fun onDestroy() {
        sensorMonitor.stop()
        super.onDestroy()
    }
}

enum class WearScreen {
    Dashboard,
    Attendance,
    Sensors,
    Notifications
}

data class PairingStatus(
    val success: Boolean,
    val linked: Boolean,
    val message: String,
    val userName: String = "",
    val email: String = ""
)

data class PairingResult(
    val success: Boolean,
    val message: String,
    val userName: String = "",
    val email: String = ""
)

data class UiNotification(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val leida: Boolean
)

@Composable
fun SmartRhWearApp(
    repository: WearableRepository,
    sensorMonitor: SensorMonitor,
    deviceInfoProvider: DeviceInfoProvider,
    locationProvider: LocationProvider
) {
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(WearScreen.Dashboard) }
    var statusMessage by remember { mutableStateOf("Verificando vinculación...") }
    var lastEvent by remember { mutableStateOf("Sin registros") }
    var lastLocation by remember { mutableStateOf<LocationPayload?>(null) }
    var battery by remember { mutableStateOf(deviceInfoProvider.getBatteryPercent()) }
    var loading by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<UiNotification>>(emptyList()) }

    var isPaired by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }
    var pairedUserName by remember { mutableStateOf("") }
    var pairedEmail by remember { mutableStateOf("") }

    val accelerometer = sensorMonitor.accelerometerPayload

    fun checkPairing(showMessage: Boolean = true) {
        if (loading) return

        scope.launch {
            loading = true

            if (showMessage) {
                statusMessage = "Revisando vínculo..."
            }

            val result = verificarVinculacionWearable()

            isPaired = result.linked
            pairedUserName = result.userName
            pairedEmail = result.email

            statusMessage = if (result.linked) {
                "Vínculo activo con SMART RH."
            } else {
                result.message
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        checkPairing(showMessage = false)
    }

    LaunchedEffect(Unit) {
        while (true) {
            battery = deviceInfoProvider.getBatteryPercent()
            delay(5000)
        }
    }

    fun pairDevice() {
        if (loading) return

        val code = pairingCode.trim()

        if (code.length != 6) {
            statusMessage = "Ingresa el código de 6 dígitos."
            return
        }

        scope.launch {
            loading = true
            statusMessage = "Vinculando cuenta..."

            val result = vincularRelojConCodigo(
                codigo = code,
                bateria = battery
            )

            if (result.success) {
                isPaired = true
                pairedUserName = result.userName
                pairedEmail = result.email
                pairingCode = ""
            }

            statusMessage = result.message
            loading = false
        }
    }

    fun sendAttendance(type: String) {
        if (loading) return

        scope.launch {
            loading = true
            statusMessage = "Enviando $type..."

            val location = locationProvider.getCurrentLocation()
            lastLocation = location

            val result = repository.enviarEvento(
                tipoEvento = type,
                location = location,
                accelerometer = accelerometer,
                battery = battery
            )

            statusMessage = if (result.success) {
                "$type registrado correctamente."
            } else {
                result.message
            }

            lastEvent = if (result.success) type else "Error"
            loading = false
        }
    }

    fun sendSensorSnapshot() {
        if (loading) return

        scope.launch {
            loading = true
            statusMessage = "Enviando sensores..."

            val location = locationProvider.getCurrentLocation()
            lastLocation = location

            val result = repository.enviarSensores(
                location = location,
                accelerometer = accelerometer,
                battery = battery
            )

            statusMessage = if (result.success) {
                "Sensores enviados correctamente."
            } else {
                result.message
            }

            loading = false
        }
    }

    fun loadNotifications() {
        if (loading) return

        scope.launch {
            loading = true
            statusMessage = "Actualizando avisos..."

            val result = cargarNotificacionesWearable()

            result
                .onSuccess {
                    notifications = it
                    statusMessage = if (it.isEmpty()) {
                        "No hay avisos nuevos."
                    } else {
                        "Avisos actualizados: ${it.size}"
                    }
                }
                .onFailure {
                    notifications = emptyList()
                    statusMessage = "No se pudieron cargar avisos."
                }

            loading = false
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF06111F),
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
        ) {
            if (!isPaired) {
                PairingScreen(
                    pairingCode = pairingCode,
                    loading = loading,
                    statusMessage = statusMessage,
                    onDigit = { digit ->
                        if (pairingCode.length < 6 && !loading) {
                            pairingCode += digit
                        }
                    },
                    onDelete = {
                        if (pairingCode.isNotEmpty() && !loading) {
                            pairingCode = pairingCode.dropLast(1)
                        }
                    },
                    onClear = {
                        if (!loading) {
                            pairingCode = ""
                        }
                    },
                    onPair = { pairDevice() },
                    onCheck = { checkPairing() }
                )
            } else {
                when (currentScreen) {
                    WearScreen.Dashboard -> DashboardScreen(
                        statusMessage = statusMessage,
                        battery = battery,
                        lastEvent = lastEvent,
                        loading = loading,
                        userName = pairedUserName,
                        email = pairedEmail,
                        onNavigate = { currentScreen = it }
                    )

                    WearScreen.Attendance -> AttendanceScreen(
                        loading = loading,
                        lastEvent = lastEvent,
                        statusMessage = statusMessage,
                        onSendAttendance = ::sendAttendance,
                        onBack = { currentScreen = WearScreen.Dashboard }
                    )

                    WearScreen.Sensors -> SensorsScreen(
                        battery = battery,
                        accelerometerX = accelerometer.x,
                        accelerometerY = accelerometer.y,
                        accelerometerZ = accelerometer.z,
                        movement = accelerometer.movement,
                        lastLocation = lastLocation,
                        loading = loading,
                        statusMessage = statusMessage,
                        onSendSnapshot = ::sendSensorSnapshot,
                        onBack = { currentScreen = WearScreen.Dashboard }
                    )

                    WearScreen.Notifications -> NotificationsScreen(
                        notifications = notifications,
                        loading = loading,
                        statusMessage = statusMessage,
                        onRefresh = ::loadNotifications,
                        onBack = { currentScreen = WearScreen.Dashboard }
                    )
                }
            }
        }
    }
}

@Composable
fun PairingScreen(
    pairingCode: String,
    loading: Boolean,
    statusMessage: String,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onPair: () -> Unit,
    onCheck: () -> Unit
) {
    WearContainer {
        item {
            BrandHeader(
                title = "Conectar",
                subtitle = "Cuenta SMART RH"
            )
        }

        item {
            MetricCard(
                label = "Código",
                value = pairingCode.padEnd(6, '•'),
                detail = "Genera el código desde SMART RH y escríbelo aquí."
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitButton("1", Modifier.weight(1f)) { onDigit("1") }
                    DigitButton("2", Modifier.weight(1f)) { onDigit("2") }
                    DigitButton("3", Modifier.weight(1f)) { onDigit("3") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitButton("4", Modifier.weight(1f)) { onDigit("4") }
                    DigitButton("5", Modifier.weight(1f)) { onDigit("5") }
                    DigitButton("6", Modifier.weight(1f)) { onDigit("6") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitButton("7", Modifier.weight(1f)) { onDigit("7") }
                    DigitButton("8", Modifier.weight(1f)) { onDigit("8") }
                    DigitButton("9", Modifier.weight(1f)) { onDigit("9") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitButton("Borrar", Modifier.weight(1f)) { onDelete() }
                    DigitButton("0", Modifier.weight(1f)) { onDigit("0") }
                    DigitButton("Limpiar", Modifier.weight(1f)) { onClear() }
                }
            }
        }

        item {
            ActionButton(
                text = if (loading) "Conectando..." else "Vincular",
                color = Color(0xFF2DD4BF),
                onClick = onPair
            )
        }

        item {
            ActionButton(
                text = "Revisar",
                color = Color(0xFF38BDF8),
                onClick = onCheck
            )
        }

        item {
            Text(
                text = statusMessage,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashboardScreen(
    statusMessage: String,
    battery: Int,
    lastEvent: String,
    loading: Boolean,
    userName: String,
    email: String,
    onNavigate: (WearScreen) -> Unit
) {
    WearContainer {
        item {
            BrandHeader(
                title = "SMART RH",
                subtitle = "Wear OS · ${ApiConfig.DEFAULT_DEVICE_ID}"
            )
        }

        item {
            MetricCard(
                label = "Cuenta",
                value = userName.ifBlank { "Usuario SMART RH" },
                detail = email.ifBlank { "Cuenta vinculada correctamente." }
            )
        }

        item {
            MetricCard(
                label = "Estado",
                value = if (loading) "Sincronizando" else "Conectado",
                detail = statusMessage
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallMetric(
                    label = "Batería",
                    value = "$battery%",
                    modifier = Modifier.weight(1f)
                )

                SmallMetric(
                    label = "Último",
                    value = lastEvent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            NavigationGrid(onNavigate = onNavigate)
        }
    }
}

@Composable
fun AttendanceScreen(
    loading: Boolean,
    lastEvent: String,
    statusMessage: String,
    onSendAttendance: (String) -> Unit,
    onBack: () -> Unit
) {
    WearContainer {
        item {
            BrandHeader(
                title = "Asistencia",
                subtitle = "Entrada · Salida · Alerta"
            )
        }

        item {
            MetricCard(
                label = "Último evento",
                value = lastEvent,
                detail = statusMessage
            )
        }

        item {
            ActionButton(
                text = if (loading) "Enviando..." else "Entrada",
                color = Color(0xFF22C55E),
                onClick = { onSendAttendance("ENTRADA") }
            )
        }

        item {
            ActionButton(
                text = if (loading) "Enviando..." else "Salida",
                color = Color(0xFF38BDF8),
                onClick = { onSendAttendance("SALIDA") }
            )
        }

        item {
            ActionButton(
                text = if (loading) "Enviando..." else "Alerta RH",
                color = Color(0xFFEF4444),
                onClick = { onSendAttendance("ALERTA") }
            )
        }

        item {
            BackButton(onBack)
        }
    }
}

@Composable
fun SensorsScreen(
    battery: Int,
    accelerometerX: Float,
    accelerometerY: Float,
    accelerometerZ: Float,
    movement: Double,
    lastLocation: LocationPayload?,
    loading: Boolean,
    statusMessage: String,
    onSendSnapshot: () -> Unit,
    onBack: () -> Unit
) {
    WearContainer {
        item {
            BrandHeader(
                title = "Sensores",
                subtitle = "GPS · Movimiento · Batería"
            )
        }

        item {
            MetricCard(
                label = "Movimiento",
                value = String.format("%.2f", movement),
                detail = "X ${String.format("%.1f", accelerometerX)} · Y ${String.format("%.1f", accelerometerY)} · Z ${String.format("%.1f", accelerometerZ)}"
            )
        }

        item {
            MetricCard(
                label = "Ubicación",
                value = if (lastLocation?.latitude != null) "GPS activo" else "Sin lectura",
                detail = if (lastLocation?.latitude != null) {
                    "${String.format("%.4f", lastLocation.latitude)} , ${String.format("%.4f", lastLocation.longitude)}"
                } else {
                    "Permite ubicación y envía una lectura."
                }
            )
        }

        item {
            SmallMetric(
                label = "Batería",
                value = "$battery%",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ActionButton(
                text = if (loading) "Enviando..." else "Enviar sensores",
                color = Color(0xFF2DD4BF),
                onClick = onSendSnapshot
            )
        }

        item {
            Text(
                text = statusMessage,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        item {
            BackButton(onBack)
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<UiNotification>,
    loading: Boolean,
    statusMessage: String,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    WearContainer {
        item {
            BrandHeader(
                title = "Avisos",
                subtitle = "Portal SMART RH"
            )
        }

        item {
            ActionButton(
                text = if (loading) "Actualizando..." else "Actualizar avisos",
                color = Color(0xFF38BDF8),
                onClick = onRefresh
            )
        }

        if (notifications.isEmpty()) {
            item {
                MetricCard(
                    label = "Sin avisos",
                    value = "0",
                    detail = statusMessage
                )
            }
        } else {
            notifications.take(6).forEach { notification ->
                item {
                    MetricCard(
                        label = notification.tipo,
                        value = if (notification.leida) "Leída" else "Nueva",
                        detail = "${notification.titulo}: ${notification.mensaje}"
                    )
                }
            }
        }

        item {
            BackButton(onBack)
        }
    }
}

@Composable
fun WearContainer(content: androidx.wear.compose.material.ScalingLazyListScope.() -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 26.dp,
            start = 12.dp,
            end = 12.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun BrandHeader(title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2DD4BF),
                            Color(0xFF38BDF8)
                        )
                    )
                )
        ) {
            Text(
                text = "SRH",
                color = Color(0xFF03111A),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111C2E))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF2DD4BF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = detail,
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0B1526))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF38BDF8),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(
            text = text,
            color = Color(0xFF03111A),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DigitButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF17263A))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (text.length > 1) 8.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    ActionButton(
        text = "Volver",
        color = Color(0xFF475569),
        onClick = onBack
    )
}

@Composable
fun NavigationGrid(onNavigate: (WearScreen) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavItemButton(
                text = "Asistencia",
                modifier = Modifier.weight(1f)
            ) {
                onNavigate(WearScreen.Attendance)
            }

            NavItemButton(
                text = "Sensores",
                modifier = Modifier.weight(1f)
            ) {
                onNavigate(WearScreen.Sensors)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavItemButton(
                text = "Avisos",
                modifier = Modifier.fillMaxWidth()
            ) {
                onNavigate(WearScreen.Notifications)
            }
        }
    }
}

@Composable
fun NavItemButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF17263A))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

suspend fun verificarVinculacionWearable(): PairingStatus {
    return withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.BASE_URL}/wearables/device/${ApiConfig.DEFAULT_DEVICE_ID}"
            val response = httpGet(url)

            val linked = response.contains("\"vinculado\":true")
            val nombre = extractJsonString(response, "nombre")
            val apellido = extractJsonString(response, "apellido")
            val correo = extractJsonString(response, "correo")

            if (linked) {
                PairingStatus(
                    success = true,
                    linked = true,
                    message = "Vínculo activo con SMART RH.",
                    userName = "$nombre $apellido".trim().ifBlank { "Usuario SMART RH" },
                    email = correo
                )
            } else {
                PairingStatus(
                    success = true,
                    linked = false,
                    message = "Genera un código desde SMART RH."
                )
            }
        } catch (error: Exception) {
            PairingStatus(
                success = false,
                linked = false,
                message = "No se pudo conectar con SMART RH."
            )
        }
    }
}

suspend fun vincularRelojConCodigo(codigo: String, bateria: Int): PairingResult {
    return withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.BASE_URL}/wearables/pair"

            val body = """
                {
                  "codigo": "${jsonEscape(codigo)}",
                  "device_id": "${jsonEscape(ApiConfig.DEFAULT_DEVICE_ID)}",
                  "nombre_dispositivo": "${jsonEscape(ApiConfig.DEFAULT_DEVICE_NAME)}",
                  "modelo": "${jsonEscape(ApiConfig.DEFAULT_MODEL)}",
                  "plataforma": "${jsonEscape(ApiConfig.DEFAULT_PLATFORM)}",
                  "bateria": $bateria
                }
            """.trimIndent()

            val response = httpPostJson(url, body)
            val ok = response.contains("\"ok\":true")
            val message = extractJsonString(response, "message")
                .ifBlank { if (ok) "Reloj vinculado correctamente." else "No se pudo vincular." }

            if (!ok) {
                return@withContext PairingResult(
                    success = false,
                    message = message
                )
            }

            val status = verificarVinculacionWearable()

            PairingResult(
                success = true,
                message = "Reloj vinculado correctamente.",
                userName = status.userName,
                email = status.email
            )
        } catch (error: Exception) {
            PairingResult(
                success = false,
                message = "Error de conexión. Revisa backend o BASE_URL."
            )
        }
    }
}

suspend fun cargarNotificacionesWearable(): Result<List<UiNotification>> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.BASE_URL}/wearables/notificaciones/${ApiConfig.DEFAULT_DEVICE_ID}"
            val response = httpGet(url)

            if (!response.contains("\"ok\":true")) {
                return@withContext Result.failure(Exception("Respuesta inválida del backend."))
            }

            val notifications = parseNotifications(response)

            Result.success(notifications)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

fun parseNotifications(json: String): List<UiNotification> {
    val arrayContent = extractJsonArray(json, "notificaciones")

    if (arrayContent.isBlank()) {
        return emptyList()
    }

    val objects = Regex("\\{[^{}]*\\}").findAll(arrayContent).map { it.value }.toList()

    return objects.mapNotNull { item ->
        val id = extractJsonInt(item, "id")
        val titulo = extractJsonString(item, "titulo")
        val mensaje = extractJsonString(item, "mensaje")
        val tipo = extractJsonString(item, "tipo").ifBlank { "INFO" }
        val leida = extractJsonBooleanOrNumber(item, "leida")

        if (titulo.isBlank() && mensaje.isBlank()) {
            null
        } else {
            UiNotification(
                id = id,
                titulo = titulo,
                mensaje = mensaje,
                tipo = tipo,
                leida = leida
            )
        }
    }
}

fun extractJsonArray(json: String, key: String): String {
    val keyIndex = json.indexOf("\"$key\"")

    if (keyIndex == -1) {
        return ""
    }

    val start = json.indexOf("[", keyIndex)

    if (start == -1) {
        return ""
    }

    var depth = 0

    for (i in start until json.length) {
        when (json[i]) {
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) {
                    return json.substring(start + 1, i)
                }
            }
        }
    }

    return ""
}

fun httpGet(urlText: String): String {
    val connection = URL(urlText).openConnection() as HttpURLConnection

    return try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        stream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

fun httpPostJson(urlText: String, body: String): String {
    val connection = URL(urlText).openConnection() as HttpURLConnection

    return try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        OutputStreamWriter(connection.outputStream).use {
            it.write(body)
            it.flush()
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        stream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

fun extractJsonString(json: String, key: String): String {
    val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
    return pattern.find(json)?.groupValues?.getOrNull(1)
        ?.replace("\\\"", "\"")
        ?.replace("\\n", "\n")
        ?: ""
}

fun extractJsonInt(json: String, key: String): Int {
    val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
    return pattern.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
}

fun extractJsonBooleanOrNumber(json: String, key: String): Boolean {
    val pattern = Regex("\"$key\"\\s*:\\s*(true|false|1|0)", RegexOption.IGNORE_CASE)
    val value = pattern.find(json)?.groupValues?.getOrNull(1)?.lowercase() ?: "false"

    return value == "true" || value == "1"
}

fun jsonEscape(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
}