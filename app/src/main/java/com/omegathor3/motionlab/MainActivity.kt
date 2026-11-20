package com.omegathor3.motionlab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Night = Color(0xFF070B16)
private val Panel = Color(0xFF111A2B)
private val Panel2 = Color(0xFF17243A)
private val Cyan = Color(0xFF27E4F2)
private val Violet = Color(0xFF9B79FF)
private val Muted = Color(0xFF9BAAC3)
private val Green = Color(0xFF4BE3A2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 22)
        setContent { MotionLabScreen(this) }
    }
}

@Composable
private fun MotionLabScreen(context: Context) {
    var speed by remember { mutableIntStateOf(5) }
    var steps by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var acceleration by remember { mutableStateOf("في انتظار قراءة المستشعر…") }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val sensors = remember {
        listOf(
            "مقياس التسارع" to Sensor.TYPE_ACCELEROMETER,
            "عداد الخطوات" to Sensor.TYPE_STEP_COUNTER,
            "كاشف الخطوات" to Sensor.TYPE_STEP_DETECTOR,
            "الجيروسكوب" to Sensor.TYPE_GYROSCOPE
        ).map { (label, type) -> label to (sensorManager.getDefaultSensor(type) != null) }
    }

    DisposableEffect(accelerometer) {
        if (accelerometer == null) {
            acceleration = "مستشعر التسارع غير متاح على هذا الهاتف"
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    acceleration = "X: %.2f     Y: %.2f     Z: %.2f m/s²".format(
                        event.values[0], event.values[1], event.values[2]
                    )
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    LaunchedEffect(running, speed) {
        while (running) {
            delay((60_000L / (speed * 22).coerceAtLeast(1)).coerceAtLeast(100L))
            steps += 1
        }
    }

    val darkColors = darkColorScheme(
        primary = Cyan,
        secondary = Violet,
        background = Night,
        surface = Panel,
        onPrimary = Night,
        onBackground = Color(0xFFF3F7FF),
        onSurface = Color(0xFFF3F7FF)
    )

    MaterialTheme(colorScheme = darkColors) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF101B31), Night, Color(0xFF090D19)))
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).background(
                            Brush.linearGradient(listOf(Cyan, Violet)), RoundedCornerShape(15.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("〽", color = Night, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MotionLab", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("STEP SIMULATOR  •  SENSOR LAB", fontSize = 9.sp, color = Muted, letterSpacing = 1.4.sp)
                    }
                    Box(Modifier.size(10.dp).background(if (running) Green else Muted, CircleShape))
                }

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(Color(0xFF12384A), Color(0xFF171B39), Panel))
                        ).padding(22.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("عداد الخطوات الافتراضي", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("%,d".format(steps), fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("خطوة", color = Cyan, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Metric("السرعة", "$speed", "كم/س")
                                Metric("الحالة", if (running) "نشطة" else "متوقفة", "")
                                Metric("المصدر", "محلي", "داخل التطبيق")
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("التحكم في المحاكاة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("سرعة الحركة", color = Muted)
                            Spacer(Modifier.weight(1f))
                            Text("$speed كم/ساعة", color = Cyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speed.toFloat(),
                            onValueChange = { speed = it.roundToInt().coerceIn(1, 20) },
                            valueRange = 1f..20f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { running = !running },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Night)
                            ) {
                                Text(if (running) "إيقاف مؤقت" else "بدء المحاكاة", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { running = false; steps = 0 },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                            ) { Text("إعادة ضبط") }
                        }
                        Text(
                            "المحاكاة تجريبية ومحلية؛ لا تغيّر عداد Android ولا ترسل خطوات لتطبيقات أخرى.",
                            color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }

                Text("مختبر المستشعرات", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("قراءة التسارع المباشرة", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("LIVE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(acceleration, color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("حرّك الهاتف برفق لملاحظة تغيّر المحاور.", color = Muted, fontSize = 12.sp)
                    }
                }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        sensors.forEach { (name, available) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(if (available) Green else Color(0xFFFF647C), CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(name, modifier = Modifier.weight(1f), color = Color.White)
                                Text(if (available) "متاح" else "غير متاح", color = if (available) Green else Color(0xFFFF8294), fontSize = 12.sp)
                            }
                        }
                    }
                }
                Text(
                    "MOTIONLAB  •  نسخة تجريبية",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (unit.isNotEmpty()) Text(unit, color = Muted, fontSize = 10.sp)
    }
}
