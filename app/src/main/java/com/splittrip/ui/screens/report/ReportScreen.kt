package com.splittrip.ui.screens.report

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splittrip.data.repository.MemberSummary
import com.splittrip.data.repository.Settlement
import com.splittrip.ui.theme.Primary
import com.splittrip.ui.theme.Success
import com.splittrip.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDetailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currency = trip?.currency ?: "USD"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settlement Report", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = viewModel.generateShareText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            putExtra(Intent.EXTRA_SUBJECT, "${trip?.tripName} - Settlement")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Report"))
                    }) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        val currentReport = report
        if (currentReport == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data available")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Primary, Color(0xFF9C88FF))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(trip?.tripName ?: "Trip", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Final Settlement Report", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(12.dp))
                        Row {
                            val totalAmount = currentReport.memberSummaries.sumOf { it.totalConsumed }
                            InfoChip("Total: $currency ${String.format("%.2f", totalAmount)}")
                            Spacer(Modifier.width(8.dp))
                            InfoChip("${currentReport.settlements.size} transactions needed")
                        }
                    }
                }
            }

            // View toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf(false to "Simple View", true to "Detailed View").forEach { (isDetailed, label) ->
                        Button(
                            onClick = { showDetailed = isDetailed },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showDetailed == isDetailed) Primary else Color.Transparent,
                                contentColor = if (showDetailed == isDetailed) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(label, fontSize = 13.sp) }
                    }
                }
            }

            if (!showDetailed) {
                // Simple View - settlements
                if (currentReport.settlements.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("All Settled!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Success)
                                Text("Everyone is even", color = Success.copy(alpha = 0.7f))
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "PAYMENTS TO MAKE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    items(currentReport.settlements) { settlement ->
                        SettlementCard(settlement = settlement, currency = currency)
                    }
                }
            } else {
                // Detailed view - member summaries
                item {
                    Text(
                        "MEMBER BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                items(currentReport.memberSummaries) { summary ->
                    MemberSummaryCard(summary = summary, currency = currency)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettlementCard(settlement: Settlement, currency: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(44.dp).background(Error.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(settlement.fromMemberName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Error, fontSize = 16.sp)
                }
                Text(settlement.fromMemberName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.ArrowForward, null, tint = Primary, modifier = Modifier.size(24.dp))
                Text(
                    "$currency ${String.format("%.2f", settlement.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(44.dp).background(Success.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(settlement.toMemberName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Success, fontSize = 16.sp)
                }
                Text(settlement.toMemberName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun MemberSummaryCard(summary: MemberSummary, currency: String) {
    val balance = summary.balance
    val balanceColor = when {
        balance > 0.01 -> Success
        balance < -0.01 -> Error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val balanceLabel = when {
        balance > 0.01 -> "Gets Back"
        balance < -0.01 -> "Owes"
        else -> "Settled"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(summary.member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary)
                }
                Spacer(Modifier.width(12.dp))
                Text(summary.member.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(balanceLabel, fontSize = 11.sp, color = balanceColor)
                    Text(
                        "$currency ${String.format("%.2f", Math.abs(balance))}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBox(label = "Total Paid", value = "$currency ${String.format("%.2f", summary.totalPaid)}", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                StatBox(label = "Total Consumed", value = "$currency ${String.format("%.2f", summary.totalConsumed)}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color.White)
    }
}
