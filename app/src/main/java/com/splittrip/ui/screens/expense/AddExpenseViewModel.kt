package com.splittrip.ui.screens.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splittrip.data.entities.Expense
import com.splittrip.data.entities.Member
import com.splittrip.data.repository.SplitTripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExpenseUiState(
    val description: String = "",
    val totalAmount: String = "",
    val members: List<Member> = emptyList(),
    val participantIds: Set<String> = emptySet(),
    val payerAmounts: Map<String, String> = emptyMap(),
    val isMultiPayer: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val existingExpenseId: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: SplitTripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val expenseId: String? = savedStateHandle.get<String>("expenseId")?.takeIf { s -> s.isNotBlank() }

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val members = repository.getMembersByTrip(tripId).first()
            val allMemberIds = members.map { it.memberId }.toSet()

            if (expenseId != null) {
                val expense = repository.getExpensesByTrip(tripId).first().find { it.expenseId == expenseId }
                if (expense != null) {
                    _uiState.value = AddExpenseUiState(
                        description = expense.description,
                        totalAmount = expense.totalAmount.toString(),
                        members = members,
                        participantIds = expense.participants.toSet(),
                        payerAmounts = expense.paidBy.mapValues { entry -> entry.value.toString() },
                        isMultiPayer = expense.paidBy.size > 1,
                        isEditMode = true,
                        existingExpenseId = expenseId
                    )
                    return@launch
                }
            }

            _uiState.value = AddExpenseUiState(
                members = members,
                participantIds = allMemberIds,
                payerAmounts = if (members.isNotEmpty()) mapOf(members[0].memberId to "") else emptyMap()
            )
        }
    }

    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc, error = null) }
    }

    fun setTotalAmount(amount: String) {
        _uiState.update { it.copy(totalAmount = amount, error = null) }
    }

    fun toggleParticipant(memberId: String) {
        _uiState.update { state ->
            val newParticipants = if (memberId in state.participantIds) {
                state.participantIds - memberId
            } else {
                state.participantIds + memberId
            }
            state.copy(participantIds = newParticipants)
        }
    }

    fun toggleMultiPayer() {
        _uiState.update { state ->
            val newIsMulti = !state.isMultiPayer
            if (!newIsMulti) {
                val firstPayer = state.payerAmounts.keys.firstOrNull() ?: state.members.firstOrNull()?.memberId
                val newPayerAmounts = if (firstPayer != null) mapOf(firstPayer to "") else emptyMap()
                state.copy(isMultiPayer = false, payerAmounts = newPayerAmounts)
            } else {
                state.copy(isMultiPayer = true)
            }
        }
    }

    fun setSinglePayer(memberId: String) {
        _uiState.update { it.copy(payerAmounts = mapOf(memberId to "")) }
    }

    fun setPayerAmount(memberId: String, amount: String) {
        _uiState.update { state ->
            val newAmounts = state.payerAmounts.toMutableMap()
            if (amount.isBlank()) {
                newAmounts.remove(memberId)
            } else {
                newAmounts[memberId] = amount
            }
            state.copy(payerAmounts = newAmounts)
        }
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val totalAmount = state.totalAmount.toDoubleOrNull()

        if (state.description.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a description") }
            return
        }
        if (totalAmount == null || totalAmount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        if (state.participantIds.isEmpty()) {
            _uiState.update { it.copy(error = "At least one participant is required") }
            return
        }
        if (state.payerAmounts.isEmpty()) {
            _uiState.update { it.copy(error = "At least one payer is required") }
            return
        }

        val paidByMap: Map<String, Double> = if (state.isMultiPayer) {
            val amounts = state.payerAmounts.mapValues { entry -> entry.value.toDoubleOrNull() ?: 0.0 }
                .filter { entry -> entry.value > 0 }
            val sum = amounts.values.sum()
            if (Math.abs(sum - totalAmount) > 0.01) {
                _uiState.update { it.copy(error = "Payer amounts (${String.format("%.2f", sum)}) must equal total (${String.format("%.2f", totalAmount)})") }
                return
            }
            amounts
        } else {
            val payerId = state.payerAmounts.keys.firstOrNull()
            if (payerId == null) {
                _uiState.update { it.copy(error = "Please select a payer") }
                return
            }
            mapOf(payerId to totalAmount)
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val expense = Expense(
                expenseId = state.existingExpenseId ?: java.util.UUID.randomUUID().toString(),
                tripId = tripId,
                description = state.description.trim(),
                totalAmount = totalAmount,
                paidBy = paidByMap,
                participants = state.participantIds.toList()
            )

            if (state.isEditMode) {
                repository.updateExpense(expense)
            } else {
                repository.addExpense(expense)
            }
            onSuccess()
        }
    }
}
