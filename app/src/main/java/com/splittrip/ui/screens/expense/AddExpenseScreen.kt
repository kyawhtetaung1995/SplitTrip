package com.splittrip.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splittrip.data.entities.Member
import com.splittrip.ui.theme.Primary
import com.splittrip.ui.theme.Success
import com.splittrip.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Expense" else "Add Expense",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description and Amount
            item {
                SectionLabel("Expense Details")
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::setDescription,
                            label = { Text("Description / Shop Name") },
                            placeholder = { Text("e.g. Dinner at Le Bistro") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Store, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.totalAmount,
                            onValueChange = viewModel::setTotalAmount,
                            label = { Text("Total Amount") },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.AttachMoney, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }

            // Payer section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Who Paid?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Multi-Payer", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = uiState.isMultiPayer,
                            onCheckedChange = { viewModel.toggleMultiPayer() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!uiState.isMultiPayer) {
                            // Single payer - dropdown selector
                            val selectedPayerId = uiState.payerAmounts.keys.firstOrNull()
                            uiState.members.forEachIndexed { index, member ->
                                SinglePayerRow(
                                    member = member,
                                    isSelected = member.memberId == selectedPayerId,
                                    onSelect = { viewModel.setSinglePayer(member.memberId) }
                                )
                                if (index < uiState.members.size - 1) {
                                    Divider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        } else {
                            // Multi payer - amount inputs
                            val totalEntered = uiState.payerAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                            val totalNeeded = uiState.totalAmount.toDoubleOrNull() ?: 0.0
                            val remaining = totalNeeded - totalEntered

                            if (totalNeeded > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (Math.abs(remaining) < 0.01) Success.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining:", fontSize = 12.sp)
                                    Text(
                                        String.format("%.2f", remaining),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (Math.abs(remaining) < 0.01) Success else Error
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            uiState.members.forEach { member ->
                                MultiPayerRow(
                                    member = member,
                                    amount = uiState.payerAmounts[member.memberId] ?: "",
                                    onAmountChange = { viewModel.setPayerAmount(member.memberId, it) }
                                )
                            }
                        }
                    }
                }
            }

            // Participants section
            item {
                val participantCount = uiState.participantIds.size
                val sharePerPerson = if (participantCount > 0 && uiState.totalAmount.toDoubleOrNull() != null) {
                    (uiState.totalAmount.toDouble()) / participantCount
                } else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            "Who Participated?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (sharePerPerson != null) {
                            Text(
                                "${participantCount} people • ${String.format("%.2f", sharePerPerson)} each",
                                fontSize = 11.sp,
                                color = Primary
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            // Toggle all
                            if (uiState.participantIds.size == uiState.members.size) {
                                uiState.members.forEach { viewModel.toggleParticipant(it.memberId) }
                            } else {
                                uiState.members.filter { it.memberId !in uiState.participantIds }
                                    .forEach { viewModel.toggleParticipant(it.memberId) }
                            }
                        }
                    ) {
                        Text(if (uiState.participantIds.size == uiState.members.size) "Deselect All" else "Select All", fontSize = 12.sp)
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.members.forEachIndexed { index, member ->
                            ParticipantRow(
                                member = member,
                                isChecked = member.memberId in uiState.participantIds,
                                onToggle = { viewModel.toggleParticipant(member.memberId) }
                            )
                            if (index < uiState.members.size - 1) {
                                Divider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Save button
            item {
                Button(
                    onClick = { viewModel.saveExpense(onNavigateBack) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(if (uiState.isEditMode) Icons.Filled.Save else Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isEditMode) "Save Changes" else "Add Expense",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
fun SinglePayerRow(member: Member, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(if (isSelected) Primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = Primary)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(member.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun MultiPayerRow(member: Member, amount: String, onAmountChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(member.name, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            placeholder = { Text("0.00") },
            modifier = Modifier.width(100.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}

@Composable
fun ParticipantRow(member: Member, isChecked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .background(if (isChecked) Primary.copy(alpha = 0.05f) else Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(member.name, fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal)
    }
}
