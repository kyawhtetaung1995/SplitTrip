package com.splittrip.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splittrip.data.entities.Expense
import com.splittrip.data.entities.Member
import com.splittrip.ui.theme.Primary
import com.splittrip.ui.theme.Success
import com.splittrip.ui.theme.Error
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onNavigateBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onEditExpense: (String, String) -> Unit,
    onViewReport: (String) -> Unit,
    onManageMembers: (String) -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val trip by viewModel.trip.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val members by viewModel.members.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Expense?>(null) }

    val totalExpenses = expenses.sumOf { it.totalAmount }
    val memberMap = members.associateBy { it.memberId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(trip?.tripName ?: "", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${members.size} members • ${trip?.currency ?: "USD"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onManageMembers(viewModel.getTripId()) }) {
                        Icon(Icons.Filled.Group, "Members")
                    }
                    IconButton(onClick = { onViewReport(viewModel.getTripId()) }) {
                        Icon(Icons.Filled.Assessment, "Report")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpense(viewModel.getTripId()) },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add Expense", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary card
            item {
                SummaryCard(
                    totalExpenses = totalExpenses,
                    currency = trip?.currency ?: "USD",
                    memberCount = members.size,
                    expenseCount = expenses.size
                )
            }

            // Member balances
            if (members.isNotEmpty()) {
                item {
                    Text(
                        "Balances",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            members.forEachIndexed { index, member ->
                                BalanceRow(member = member, currency = trip?.currency ?: "USD")
                                if (index < members.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Expenses
            if (expenses.isNotEmpty()) {
                item {
                    Text(
                        "Expenses",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                items(expenses, key = { it.expenseId }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        memberMap = memberMap,
                        currency = trip?.currency ?: "USD",
                        onEdit = { onEditExpense(viewModel.getTripId(), expense.expenseId) },
                        onDelete = { showDeleteDialog = expense }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Receipt,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Primary.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No expenses yet",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                "Tap + to add your first expense",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    showDeleteDialog?.let { expense ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Expense") },
            text = { Text("Delete \"${expense.description}\"? This will recalculate all balances.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteExpense(expense); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SummaryCard(totalExpenses: Double, currency: String, memberCount: Int, expenseCount: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Trip Expenses", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            Text(
                "$currency ${String.format("%,.2f", totalExpenses)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row {
                StatPill(label = "Members", value = "$memberCount")
                Spacer(Modifier.width(12.dp))
                StatPill(label = "Expenses", value = "$expenseCount")
                if (memberCount > 0 && totalExpenses > 0) {
                    Spacer(Modifier.width(12.dp))
                    StatPill(
                        label = "Per Person",
                        value = "$currency ${String.format("%.2f", totalExpenses / memberCount)}"
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun BalanceRow(member: Member, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(member.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        val balance = member.totalBalance
        val color = when {
            balance > 0.01 -> Success
            balance < -0.01 -> Error
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        }
        val prefix = when {
            balance > 0.01 -> "+"
            else -> ""
        }
        Text(
            "$prefix$currency ${String.format("%.2f", balance)}",
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    memberMap: Map<String, Member>,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val payerNames = expense.paidBy.entries.joinToString(", ") { (id, amount) ->
        val name = memberMap[id]?.name ?: "Unknown"
        if (expense.paidBy.size > 1) "$name (${String.format("%.0f", amount)})" else name
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Receipt, null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Paid by $payerNames",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${expense.participants.size} participants • ${dateFormat.format(Date(expense.date))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$currency ${String.format("%.2f", expense.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
