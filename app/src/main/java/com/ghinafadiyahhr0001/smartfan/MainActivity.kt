package com.ghinafadiyahhr0001.smartfan

import android.os.Bundle
import androidx.activity. ComponentActivity
import androidx.activity. compose.setContent
import androidx. activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose.foundation. rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material. icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose. ui.text.font.FontWeight
import androidx.compose.ui. text.style.TextAlign
import androidx.compose.ui.unit. dp
import androidx.compose.ui.unit.sp
import com. ghinafadiyahhr0001.smartfan.ui. theme.SmartfanTheme
import com.google.firebase. database.*

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference. child("smartroom")

        setContent {
            SmartfanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    SmartRoomApp(database)
                }
            }
        }
    }
}

@Composable
fun SmartRoomApp(database: DatabaseReference) {
    // State variables
    var isConnected by remember { mutableStateOf(false) }
    var temperature by remember { mutableStateOf(0.0) }
    var humidity by remember { mutableStateOf(0.0) }
    var gasValue by remember { mutableStateOf(0) }
    var gasDetected by remember { mutableStateOf(false) }
    var fanStatus by remember { mutableStateOf(false) }
    var ledStatus by remember { mutableStateOf(false) }
    var manualMode by remember { mutableStateOf(false) }
    var tempThreshold by remember { mutableStateOf(30.0) }
    var humidityMinThreshold by remember { mutableStateOf(40.0) }
    var gasThreshold by remember { mutableStateOf(400) }

    // Firebase listener
    LaunchedEffect(Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot:  DataSnapshot) {
                isConnected = true

                // ✅ PERBAIKAN: Sesuaikan dengan field dari Arduino
                temperature = snapshot.child("suhu").getValue(Double::class.java) ?: 0.0
                humidity = snapshot. child("kelembaban").getValue(Double::class.java) ?: 0.0
                gasDetected = snapshot.child("gasDetected").getValue(Boolean::class. java) ?: false
                fanStatus = snapshot.child("fanStatus").getValue(Boolean::class.java) ?: false
                ledStatus = snapshot.child("ledStatus").getValue(Boolean::class.java) ?: false
                manualMode = snapshot.child("manualMode").getValue(Boolean::class. java) ?: false
                tempThreshold = snapshot.child("tempThreshold").getValue(Double::class.java) ?: 30.0
                humidityMinThreshold = snapshot. child("humidityMinThreshold").getValue(Double::class.java) ?: 40.0

                // Arduino tidak kirim gasValue (analog), hanya gasDetected (boolean)
                // Set dummy value untuk display
                gasValue = if (gasDetected) 500 else 100
                gasThreshold = 400
            }

            override fun onCancelled(error: DatabaseError) {
                isConnected = false
            }
        })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🏠 Smart Room Controller",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier
                . fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign. Center
        )

        // Connection Status Card
        StatusCard(
            title = "📊 Status Koneksi",
            isConnected = isConnected
        )

        // Temperature & Humidity Card
        Row(
            modifier = Modifier. fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "🌡 Suhu",
                value = String.format("%.1f°C", temperature),
                color = if (temperature >= tempThreshold) Color(0xFFF44336) else Color(0xFFFF5722)
            )

            SensorCard(
                modifier = Modifier.weight(1f),
                title = "💧 Kelembaban",
                value = String.format("%.1f%%", humidity),
                color = if (humidity < humidityMinThreshold) Color(0xFFF44336) else Color(0xFF2196F3)
            )
        }

        // Gas Sensor Card
        GasCard(
            gasValue = gasValue,
            gasDetected = gasDetected,
            gasThreshold = gasThreshold
        )

        // Fan Control Card
        FanControlCard(
            fanStatus = fanStatus,
            manualMode = manualMode,
            onModeChange = { isManual ->
                database.child("manualMode").setValue(isManual)
            },
            onFanToggle = {
                if (manualMode) {
                    database.child("fanStatus").setValue(!fanStatus)
                }
            }
        )

        // LED Control Card
        LEDControlCard(
            ledStatus = ledStatus,
            manualMode = manualMode,
            onLEDToggle = {
                if (manualMode) {
                    database.child("ledStatus").setValue(!ledStatus)
                }
            }
        )

        // Threshold Settings Card
        SettingsCard(
            tempThreshold = tempThreshold,
            humidityMinThreshold = humidityMinThreshold,
            gasThreshold = gasThreshold
        )

        Spacer(modifier = Modifier. height(16.dp))
    }
}

@Composable
fun StatusCard(title: String, isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults. cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults. cardColors(containerColor = Color. White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = RoundedCornerShape(50)
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    fontSize = 16.sp,
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun GasCard(
    gasValue: Int,
    gasDetected: Boolean,
    gasThreshold: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gasDetected) Color(0xFFFFEBEE) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💨 Sensor Gas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (gasDetected) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (gasDetected) Color(0xFFF44336) else Color(0xFF4CAF50),
                    modifier = Modifier. size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (gasDetected) "🔥 GAS TERDETEKSI!" else "✅ Aman",
                    fontSize = 20.sp,
                    fontWeight = if (gasDetected) FontWeight.Bold else FontWeight.Normal,
                    color = if (gasDetected) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }

            if (gasDetected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Segera nyalakan kipas untuk ventilasi! ",
                    fontSize = 14.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FanControlCard(
    fanStatus:  Boolean,
    manualMode:  Boolean,
    onModeChange: (Boolean) -> Unit,
    onFanToggle: () -> Unit
) {
    Card(
        modifier = Modifier. fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💨 Kontrol Kipas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))
// Mode Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = ! manualMode,
                    onClick = { onModeChange(false) },
                    label = { Text("🤖 Auto") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1976D2),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = manualMode,
                    onClick = { onModeChange(true) },
                    label = { Text("🔧 Manual") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1976D2),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fan Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (fanStatus) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (fanStatus) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Status:  ${if (fanStatus) "ON 💨" else "OFF ⭕️"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (fanStatus) Color(0xFF4CAF50) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Control Button
            Button(
                onClick = onFanToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = manualMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (fanStatus) Color(0xFFF44336) else Color(0xFF4CAF50),
                    disabledContainerColor = Color. LightGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (fanStatus) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (fanStatus) "Matikan Kipas" else "Nyalakan Kipas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight. Bold
                )
            }

            if (! manualMode) {
                Spacer(modifier = Modifier. height(8.dp))

                Text(
                    text = "ℹ️ Mode Auto:  Kipas dikontrol otomatis berdasarkan suhu, kelembaban & gas",
                    fontSize = 12.sp,
                    color = Color. Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier. fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LEDControlCard(
    ledStatus: Boolean,
    manualMode: Boolean,
    onLEDToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults. cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier. padding(16.dp)
        ) {
            Text(
                text = "💡 Kontrol LED",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // LED Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (ledStatus) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (ledStatus) Color(0xFFFFEB3B) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Status: ${if (ledStatus) "ON 💡" else "OFF ⭕️"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ledStatus) Color(0xFFFFEB3B) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Control Button
            Button(
                onClick = onLEDToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = manualMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ledStatus) Color(0xFFF44336) else Color(0xFFFFEB3B),
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (ledStatus) Icons.Default.Close else Icons.Default.Star,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (ledStatus) "Matikan LED" else "Nyalakan LED",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!manualMode) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ℹ️ Mode Auto: LED nyala otomatis saat gas terdeteksi",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    tempThreshold: Double,
    humidityMinThreshold: Double,
    gasThreshold: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚙️ Pengaturan Threshold",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier. fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🌡 Suhu Maks",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${tempThreshold.toInt()}°C",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                }
                Column {
                    Text(
                        text = "💧 Kelembaban Min",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${humidityMinThreshold.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}