package com.splittrip.ui.screens.addtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splittrip.data.entities.Member
import com.splittrip.data.repository.SplitTripRepository
import com.splittrip.ui.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageMembersViewModel @Inject constructor(
    private val repository: SplitTripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    val members: StateFlow<List<Member>> = repository.getMembersByTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMember(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addMember(Member(tripId = tripId, name = name.trim()))
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch { repository.deleteMember(member) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageMembersViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsState()
    var newMemberName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Member?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Members", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            label = { Text("New Member Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.addMember(newMemberName)
                                newMemberName = ""
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { viewModel.addMember(newMemberName); newMemberName = "" },
                            containerColor = Primary
                        ) { Icon(Icons.Filled.PersonAdd, null, tint = Color.White) }
                    }
                }
            }

            item {
                Text(
                    "${members.size} member${if (members.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(members, key = { it.memberId }) { member ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, fontWeight = FontWeight.Medium)
                            val balance = member.totalBalance
                            val (balText, balColor) = when {
                                balance > 0.01 -> "Gets back ${String.format("%.2f", balance)}" to com.splittrip.ui.theme.Success
                                balance < -0.01 -> "Owes ${String.format("%.2f", -balance)}" to com.splittrip.ui.theme.Error
                                else -> "Even" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            }
                            Text(balText, fontSize = 12.sp, color = balColor)
                        }
                        IconButton(onClick = { deleteTarget = member }) {
                            Icon(Icons.Filled.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { member ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove Member") },
            text = { Text("Remove ${member.name} from this trip? This will recalculate all balances.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteMember(member); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}
