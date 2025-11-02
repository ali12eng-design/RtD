
package com.example.priceconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutDirection
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val showSettings = remember { mutableStateOf(false) }
    var constants by remember { mutableStateOf(ConversionConstants()) }
    val snack = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محول الأسعار", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings.value = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFeaf3ff), Color(0xFFf7fbff))
                    )
                )
                .padding(padding)
        ) {
            PriceConverterScreen(constants) { constants = it }
            if (showSettings.value) {
                SettingsDialog(constants, onDismiss = { showSettings.value = false }) {
                    constants = it
                    showSettings.value = false
                }
            }
        }
    }
}

data class ConversionConstants(
    val rate372: BigDecimal = BigDecimal("372"),
    val cardDivisor: BigDecimal = BigDecimal("135"),
    val marketRate: BigDecimal = BigDecimal("1410")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceConverterScreen(constants: ConversionConstants, onUpdate: (ConversionConstants) -> Unit) {
    var input by remember { mutableStateOf("") }
    val df = remember { DecimalFormat("#,##0.######") }
    val clipboard = LocalClipboardManager.current

    fun parseOrZero(text: String): BigDecimal = try {
        if (text.isBlank()) BigDecimal.ZERO else BigDecimal(text.replace(",", "").trim())
    } catch (_: Exception) { BigDecimal.ZERO }

    val value = parseOrZero(input)
    val zero = BigDecimal.ZERO

    val usd = if (constants.rate372.compareTo(zero) != 0)
        value.divide(constants.rate372, 10, RoundingMode.HALF_UP) else zero
    val iqdCard = value.multiply(constants.rate372).divide(constants.cardDivisor, 10, RoundingMode.HALF_UP)
    val iqdMarket = if (constants.rate372.compareTo(zero) != 0)
        value.divide(constants.rate372, 10, RoundingMode.HALF_UP).multiply(constants.marketRate) else zero
    val afterDiscount15 = if (constants.rate372.compareTo(zero) != 0)
        value.divide(constants.rate372, 10, RoundingMode.HALF_UP).multiply(BigDecimal("0.85")) else zero

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF90caf9), Color(0xFF7dd3fc))
                    )
                )
                .padding(16.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("أدخل قيمة البضاعة", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("النتائج تُحسب مباشرة", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("ادخل قيمة البضاعة") },
            singleLine = true,
            leadingIcon = { Text("IQD") },
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ResultCard(
            title = "هذا هو السعر بالدولار",
            value = df.format(usd) + " USD",
            onCopy = { clipboard.setText(AnnotatedString(df.format(usd))) }
        )
        ResultCard(
            title = "القيمة بالدينار (بالبطاقة)",
            value = df.format(iqdCard) + " IQD",
            onCopy = { clipboard.setText(AnnotatedString(df.format(iqdCard))) }
        )
        ResultCard(
            title = "الدينار بحسب سعر السوق",
            value = df.format(iqdMarket) + " IQD",
            onCopy = { clipboard.setText(AnnotatedString(df.format(iqdMarket))) }
        )
        ResultCard(
            title = "بعد القسمة على 372 وخصم 15٪",
            value = df.format(afterDiscount15),
            onCopy = { clipboard.setText(AnnotatedString(df.format(afterDiscount15))) }
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "القيم الحالية: 372 / 135 / 1410 — يمكنك تغييرها من زر الإعدادات بالأعلى.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ResultCard(title: String, value: String, onCopy: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
        }
    }
}

@Composable
fun SettingsDialog(current: ConversionConstants, onDismiss: () -> Unit, onSave: (ConversionConstants) -> Unit) {
    var rate by remember { mutableStateOf(current.rate372.toPlainString()) }
    var card by remember { mutableStateOf(current.cardDivisor.toPlainString()) }
    var market by remember { mutableStateOf(current.marketRate.toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val new = ConversionConstants(
                    rate372 = rate.toBigDecimalOrNull() ?: current.rate372,
                    cardDivisor = card.toBigDecimalOrNull() ?: current.cardDivisor,
                    marketRate = market.toBigDecimalOrNull() ?: current.marketRate
                )
                onSave(new)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("إعدادات الأسعار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("قيمة 372") }, singleLine = true)
                OutlinedTextField(value = card, onValueChange = { card = it }, label = { Text("قيمة 135") }, singleLine = true)
                OutlinedTextField(value = market, onValueChange = { market = it }, label = { Text("قيمة 1410") }, singleLine = true)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
