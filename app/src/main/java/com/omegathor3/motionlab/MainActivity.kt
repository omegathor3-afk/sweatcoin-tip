package com.omegathor3.motionlab

import android.Manifest
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var hardwareSteps by remember { mutableStateOf<Long?>(null) }
    var detectorEvents by remember { mutableIntStateOf(0) }
    var counterBaseline by remember { mutableStateOf<Float?>(null) }
    var rawSensorTotal by remember { mutableStateOf<Float?>(null) }
    var lastSensorEventMs by remember { mutableStateOf<Long?>(null) }
    var sweatcoinNotes by remember { mutableStateOf("") }
    var hasActivityPermission by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasActivityPermission = granted }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val stepCounterSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val stepDetectorSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    val sensors = remember {
        listOf(
            "مقياس التسارع" to Sensor.TYPE_ACCELEROMETER,
            "عداد الخطوات الحقيقي" to Sensor.TYPE_STEP_COUNTER,
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

    DisposableEffect(stepCounterSensor, stepDetectorSensor, hasActivityPermission) {
        if (!hasActivityPermission) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_STEP_COUNTER -> {
                            val totalSinceBoot = event.values[0]
                            rawSensorTotal = totalSinceBoot
                            lastSensorEventMs = System.currentTimeMillis()
                            val baseline = counterBaseline
                            if (baseline == null) {
                                counterBaseline = totalSinceBoot
                                hardwareSteps = 0L
                            } else {
                                hardwareSteps = (totalSinceBoot - baseline).toLong().coerceAtLeast(0L)
                            }
                        }
                        Sensor.TYPE_STEP_DETECTOR -> { detectorEvents += 1; lastSensorEventMs = System.currentTimeMillis() }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            stepCounterSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            stepDetectorSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
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
                            Text("الخطوات الحقيقية من مستشعر الهاتف", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(hardwareSteps?.let { "%,d".format(it) } ?: "—", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (!hasActivityPermission) "يلزم إذن النشاط البدني" else "خطوة مكتشفة من مستشعر الهاتف", color = Cyan, fontWeight = FontWeight.Bold)
                            Text("عداد المحاكاة المحلي: %,d".format(steps), color = Muted, fontSize = 11.sp)
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

                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("فحص عداد الخطوات الحقيقي", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("عداد النظام: " + if (stepCounterSensor != null) "متاح" else "غير موجود" +
                            "  •  كاشف الخطوات: " + if (stepDetectorSensor != null) "متاح" else "غير موجود",
                            color = Muted, fontSize = 12.sp)
                        Text("أحداث كاشف الخطوات في هذه الجلسة: $detectorEvents", color = Cyan, fontWeight = FontWeight.SemiBold)
                        Text("العداد التراكمي منذ تشغيل الهاتف: ${rawSensorTotal?.roundToInt()?.toString() ?: "—"}", color = Muted, fontSize = 12.sp)
                        if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) {
                                Text("منح إذن النشاط البدني")
                            }
                        } else {
                            Text(if (hardwareSteps == null) "في انتظار أول قراءة من مستشعر الخطوات…" else
                                "المستشعر يعمل. امشِ بشكل طبيعي والهاتف في جيبك لاختبار العد.",
                                color = Green, fontSize = 12.sp)
                        }
                    }
                }

                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تقرير توافق Sweatcoin", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val sweatcoinVersion = remember {
                            try { context.packageManager.getPackageInfo("in.sweatco.app", 0).versionName ?: "مثبت" }
                            catch (_: Exception) { "غير مثبت أو غير ظاهر للنظام" }
                        }
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                        val batteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) powerManager.isIgnoringBatteryOptimizations(context.packageName) else true
                        Text("إصدار Sweatcoin: $sweatcoinVersion", color = Muted, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("in.sweatco.app")
                                if (launchIntent != null) context.startActivity(launchIntent)
                                else Toast.makeText(context, "تعذر فتح Sweatcoin؛ تحقق من تثبيته.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("فتح تطبيق Sweatcoin") }
                        Text("قيود بطارية MotionLab: ${if (batteryUnrestricted) "مستثنى من التحسين" else "خاضع لتحسين البطارية"}", color = Muted, fontSize = 12.sp)
                        Text("أندرويد لا يتيح قراءة بيانات Sweatcoin الخاصة مباشرة. الصق هنا النص أو الأرقام التي يعرضها التطبيق بنفسك، وسنضمّها للتقرير.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        OutlinedTextField(
                            value = sweatcoinNotes,
                            onValueChange = { sweatcoinNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("بيانات Sweatcoin المنسوخة (اختياري)") },
                            placeholder = { Text("خطوات اليوم، الخطوات المحوّلة، رسالة الخطأ…") },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                val report = buildString {
                                    appendLine("MOTIONLAB / SWEATCOIN DIAGNOSTIC")
                                    appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL}")
                                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                                    appendLine("Sweatcoin: $sweatcoinVersion")
                                    appendLine("إذن النشاط البدني: $hasActivityPermission")
                                    appendLine("Step Counter: ${stepCounterSensor != null}")
                                    appendLine("Step Detector: ${stepDetectorSensor != null}")
                                    appendLine("خطوات المستشعر في جلسة MotionLab: ${hardwareSteps ?: "لا توجد قراءة"}")
                                    appendLine("العداد التراكمي منذ تشغيل الهاتف: ${rawSensorTotal?.roundToInt() ?: "لا توجد قراءة"}")
                                    appendLine("أحداث Step Detector: $detectorEvents")
                                    appendLine("قيود بطارية MotionLab: ${if (batteryUnrestricted) "مستثنى" else "غير مستثنى"}")
                                    appendLine("المحاكاة المحلية (ليست خطوات نظام): $steps")
                                    appendLine("--- بيانات منسوخة من Sweatcoin ---")
                                    appendLine(sweatcoinNotes.ifBlank { "لم تتم إضافة بيانات" })
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MotionLab diagnostic", report))
                                Toast.makeText(context, "تم نسخ التقرير. ابعته هنا.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Night)
                        ) { Text("نسخ تقرير التشخيص لإرساله هنا", fontWeight = FontWeight.Bold) }
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
