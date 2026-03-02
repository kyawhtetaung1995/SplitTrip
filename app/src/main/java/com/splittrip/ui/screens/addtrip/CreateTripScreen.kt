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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splittrip.data.entities.Member
import com.splittrip.data.entities.Trip
import com.splittrip.data.repository.SplitTripRepository
import com.splittrip.ui.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val repository: SplitTripRepository
) : ViewModel() {
    fun createTrip(name: String, currency: String, members: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            val trip = Trip(tripName = name, currency = currency)
            repository.createTrip(trip)
            members.forEach { memberName ->
                if (memberName.isNotBlank()) {
                    repository.addMember(Member(tripId = trip.tripId, name = memberName.trim()))
                }
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onNavigateBack: () -> Unit,
    onTripCreated: (String) -> Unit,
    viewModel: CreateTripViewModel = hiltViewModel()
) {
    var tripName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var members by remember { mutableStateOf(listOf("")) }
    var newMemberName by remember { mutableStateOf("") }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    val currencies = listOf("USD", "EUR", "GBP", "JPY", "THB", "SGD", "AUD", "CAD", "MYR", "IDR")
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Trip", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            item {
                Text("Trip Details", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = tripName,
                            onValueChange = { tripName = it },
                            label = { Text("Trip Name") },
                            placeholder = { Text("e.g. Paris 2026") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Luggage, null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                        Spacer(Modifier.height(12.dp))
                        Box {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = {},
                                label = { Text("Currency") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Filled.AttachMoney, null) },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showCurrencyMenu = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = { currency = curr; showCurrencyMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Members", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newMemberName,
                                onValueChange = { newMemberName = it },
                                label = { Text("Add Member") },
                                placeholder = { Text("Enter name") },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                leadingIcon = { Icon(Icons.Filled.Person, null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newMemberName.isNotBlank()) {
                                        members = members + newMemberName.trim()
                                        newMemberName = ""
                                    }
                                })
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    if (newMemberName.isNotBlank()) {
                                        members = members + newMemberName.trim()
                                        newMemberName = ""
                                    }
                                },
                                containerColor = Primary
                            ) {
                                Icon(Icons.Filled.Add, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (members.any { it.isNotBlank() }) {
                items(members.filter { it.isNotBlank() }, key = { it + members.indexOf(it) }) { member ->
                    MemberChip(
                        name = member,
                        onDelete = { members = members.filter { it != member } }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isCreating = true
                        viewModel.createTrip(
                            name = tripName,
                            currency = currency,
                            members = members.filter { it.isNotBlank() },
                            onDone = {
                                isCreating = false
                                onNavigateBack()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = tripName.isNotBlank() && !isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Trip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberChip(name: String, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
