package com.omegathor3.motionlab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MotionLabScreen(this) }
    }
}

@Composable
private fun MotionLabScreen(context: Context) {
    var speed by remember { mutableIntStateOf(5) }
    var steps by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var acceleration by remember { mutableStateOf("في انتظار قراءة المستشعر…") }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
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
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    acceleration = "X: %.2f   Y: %.2f   Z: %.2f m/s²".format(x, y, z)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    LaunchedEffect(running, speed) {
        while (running) {
            val stepsPerMinute = (speed * 22).coerceAtLeast(1)
            delay((60_000L / stepsPerMinute).coerceAtLeast(100L))
            steps += 1
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("MOTION LAB", style = MaterialTheme.typography.headlineLarge)
                Text("مختبر محاكاة الحركة وتشخيص مستشعرات الهاتف")
                Text("المحاكاة تعمل داخل هذا التطبيق فقط، ولا تغيّر بيانات التطبيقات الأخرى.")
                HorizontalDivider()
                Text("الخطوات الافتراضية", style = MaterialTheme.typography.titleMedium)
                Text(steps.toString(), style = MaterialTheme.typography.displayLarge)
                Text("السرعة المختارة: $speed كم/ساعة")
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { speed = it.roundToInt().coerceIn(1, 20) },
                    valueRange = 1f..20f,
                    steps = 18
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { running = !running }) {
                        Text(if (running) "إيقاف المحاكاة" else "بدء المحاكاة")
                    }
                    OutlinedButton(onClick = { running = false; steps = 0 }) {
                        Text("إعادة ضبط")
                    }
                }
                HorizontalDivider()
                Text("اختبار المستشعر الحقيقي", style = MaterialTheme.typography.titleLarge)
                Text("قراءات مقياس التسارع المباشرة", style = MaterialTheme.typography.titleMedium)
                Text(acceleration, style = MaterialTheme.typography.bodyLarge)
                Text("حرّك الهاتف برفق ولاحظ تغيّر قيم X وY وZ.")
                HorizontalDivider()
                Text("حالة مستشعرات الهاتف", style = MaterialTheme.typography.titleLarge)
                sensors.forEach { (name, available) ->
                    Text("$name: ${if (available) "متاح" else "غير متاح"}")
                }
                Text(
                    "عداد المحاكاة مستقل عن عداد خطوات Android الحقيقي. لا يتم إرسال خطوات افتراضية إلى تطبيقات أخرى.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
