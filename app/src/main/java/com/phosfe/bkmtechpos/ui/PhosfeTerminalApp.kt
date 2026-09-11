package com.phosfe.bkmtechpos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phosfe.bkmtechpos.BuildConfig
import com.phosfe.bkmtechpos.domain.MinorAmount
import com.phosfe.bkmtechpos.domain.PaymentOperation

private enum class Page { HOME, AMOUNT, TRANSACTIONS, SYSTEM }

@Composable
fun PhosfeTerminalApp() {
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedOperation by remember { mutableStateOf(PaymentOperation.SALE) }
    var amountDigits by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Header()
            Box(modifier = Modifier.weight(1f)) {
                when (page) {
                    Page.HOME -> HomePage(
                        onSale = { selectedOperation = PaymentOperation.SALE; amountDigits = ""; page = Page.AMOUNT },
                        onTransactions = { page = Page.TRANSACTIONS },
                        onSystem = { page = Page.SYSTEM }
                    )
                    Page.AMOUNT -> AmountPage(
                        operation = selectedOperation,
                        digits = amountDigits,
                        onDigitsChanged = { amountDigits = it.take(10) },
                        onBack = { page = Page.HOME }
                    )
                    Page.TRANSACTIONS -> MenuPage(onSelect = {
                        selectedOperation = it
                        amountDigits = ""
                        page = Page.AMOUNT
                    }, onBack = { page = Page.HOME })
                    Page.SYSTEM -> SystemPage(onBack = { page = Page.HOME })
                }
            }
            BottomBar(page = page, onPage = { page = it })
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { Text("P", color = Color.White, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Phosfe TechPOS", style = MaterialTheme.typography.titleLarge)
            Text("Güvenli ödeme terminali", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF18A76F)))
            Spacer(Modifier.width(6.dp))
            Text("Hazır", style = MaterialTheme.typography.labelLarge, color = Color(0xFF14734F))
        }
    }
}

@Composable
private fun HomePage(onSale: () -> Unit, onTransactions: () -> Unit, onSystem: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Yeni ödeme", color = Color.White.copy(alpha = .78f))
                Spacer(Modifier.height(6.dp))
                Text("Satış işlemini\nbaşlatın", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onSale,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) { Text("Tutar gir", style = MaterialTheme.typography.titleMedium) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickCard("İşlemler", "Satış, iade, iptal", "İŞ", Modifier.weight(1f), onTransactions)
            QuickCard("Sistem", "Kurulum ve rapor", "SY", Modifier.weight(1f), onSystem)
        }
        Text("Terminal", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusLine("Donanım", BuildConfig.HARDWARE_VENDOR)
                StatusLine("Çalışma modu", if (BuildConfig.EXTERNAL_CONTROL) "ECR" else "Bağımsız")
                StatusLine("Host", BuildConfig.HOST_PRIMARY)
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, symbol: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AmountPage(operation: PaymentOperation, digits: String, onDigitsChanged: (String) -> Unit, onBack: () -> Unit) {
    val amount = MinorAmount(digits.toLongOrNull() ?: 0)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(operation.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(22.dp))
        Text(amount.display(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("İşlem tutarı", color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onDigitsChanged(if (key == "⌫") digits.dropLast(1) else digits + key) },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {}, enabled = amount.value > 0, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Kart okutmaya geç")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Vazgeç") }
    }
}

@Composable
private fun MenuPage(onSelect: (PaymentOperation) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("İşlemler", style = MaterialTheme.typography.headlineSmall)
        PaymentOperation.entries.forEach { operation ->
            Card(
                Modifier.fillMaxWidth().clickable { onSelect(operation) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(operation.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Geri") }
    }
}

@Composable
private fun SystemPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sistem merkezi", style = MaterialTheme.typography.headlineSmall)
        listOf("Terminal kurulumu", "Anahtar yönetimi", "Parametre yükleme", "Gün sonu", "Bağlantı testi", "Tanılama").forEach { title ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text(title, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Geri") }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BottomBar(page: Page, onPage: (Page) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(Page.HOME to "Ana ekran", Page.TRANSACTIONS to "İşlemler", Page.SYSTEM to "Sistem").forEach { (item, title) ->
            Text(
                title,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPage(item) }.padding(horizontal = 15.dp, vertical = 10.dp),
                color = if (page == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                fontWeight = if (page == item) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

