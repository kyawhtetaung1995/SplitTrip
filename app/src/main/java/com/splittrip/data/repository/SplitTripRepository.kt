package com.splittrip.data.repository

import com.splittrip.data.dao.ExpenseDao
import com.splittrip.data.dao.MemberDao
import com.splittrip.data.dao.TripDao
import com.splittrip.data.entities.Expense
import com.splittrip.data.entities.Member
import com.splittrip.data.entities.Trip
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

data class Settlement(
    val fromMemberId: String,
    val fromMemberName: String,
    val toMemberId: String,
    val toMemberName: String,
    val amount: Double
)

data class MemberSummary(
    val member: Member,
    val totalPaid: Double,
    val totalConsumed: Double,
    val balance: Double
)

data class TripReport(
    val settlements: List<Settlement>,
    val memberSummaries: List<MemberSummary>
)

@Singleton
class SplitTripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao
) {

    // Trip operations
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    suspend fun getTripById(tripId: String): Trip? = tripDao.getTripById(tripId)

    suspend fun createTrip(trip: Trip) = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTrip(tripId: String) {
        tripDao.deleteTripById(tripId)
        expenseDao.deleteExpensesByTrip(tripId)
        memberDao.deleteMembersByTrip(tripId)
    }

    // Member operations
    fun getMembersByTrip(tripId: String): Flow<List<Member>> = memberDao.getMembersByTrip(tripId)

    suspend fun addMember(member: Member) = memberDao.insertMember(member)

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
        // Recalculate all balances after member removal
        recalculateBalances(member.tripId)
    }

    // Expense operations
    fun getExpensesByTrip(tripId: String): Flow<List<Expense>> = expenseDao.getExpensesByTrip(tripId)

    suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
        recalculateBalances(expense.tripId)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
        recalculateBalances(expense.tripId)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        recalculateBalances(expense.tripId)
    }

    /**
     * Core Calculation Engine - recalculates all balances from scratch
     * This ensures correctness when any expense is edited or deleted
     */
    suspend fun recalculateBalances(tripId: String) {
        val members = memberDao.getMembersByTripSync(tripId)
        val expenses = expenseDao.getExpensesByTripSync(tripId)

        // Reset all balances to 0
        val balanceMap = members.associate { it.memberId to BigDecimal.ZERO }.toMutableMap()

        // Process each expense
        for (expense in expenses) {
            val numParticipants = expense.participants.size
            if (numParticipants == 0) continue

            val sharePerPerson = BigDecimal(expense.totalAmount)
                .divide(BigDecimal(numParticipants), 10, RoundingMode.HALF_UP)

            // Credit payers
            for ((memberId, amountPaid) in expense.paidBy) {
                val current = balanceMap[memberId] ?: BigDecimal.ZERO
                balanceMap[memberId] = current.add(BigDecimal(amountPaid))
            }

            // Debit participants
            for (memberId in expense.participants) {
                val current = balanceMap[memberId] ?: BigDecimal.ZERO
                balanceMap[memberId] = current.subtract(sharePerPerson)
            }
        }

        // Update all member balances
        for (member in members) {
            val balance = balanceMap[member.memberId] ?: BigDecimal.ZERO
            memberDao.updateBalance(member.memberId, balance.setScale(2, RoundingMode.HALF_UP).toDouble())
        }
    }

    /**
     * Settlement Algorithm - Minimizes number of transactions
     * Uses greedy approach matching largest debtor with largest creditor
     */
    suspend fun generateSettlements(tripId: String): TripReport {
        val members = memberDao.getMembersByTripSync(tripId)
        val expenses = expenseDao.getExpensesByTripSync(tripId)

        // Calculate member summaries
        val totalPaidMap = members.associate { it.memberId to 0.0 }.toMutableMap()
        val totalConsumedMap = members.associate { it.memberId to 0.0 }.toMutableMap()

        for (expense in expenses) {
            val numParticipants = expense.participants.size
            if (numParticipants == 0) continue
            val sharePerPerson = expense.totalAmount / numParticipants

            for ((memberId, amountPaid) in expense.paidBy) {
                totalPaidMap[memberId] = (totalPaidMap[memberId] ?: 0.0) + amountPaid
            }
            for (memberId in expense.participants) {
                totalConsumedMap[memberId] = (totalConsumedMap[memberId] ?: 0.0) + sharePerPerson
            }
        }

        val memberSummaries = members.map { member ->
            val paid = totalPaidMap[member.memberId] ?: 0.0
            val consumed = totalConsumedMap[member.memberId] ?: 0.0
            MemberSummary(
                member = member,
                totalPaid = paid,
                totalConsumed = consumed,
                balance = member.totalBalance
            )
        }

        // Settlement algorithm
        val memberMap = members.associateBy { it.memberId }

        // Use BigDecimal for precision
        val balances = members.associate {
            it.memberId to BigDecimal(it.totalBalance).setScale(2, RoundingMode.HALF_UP)
        }.toMutableMap()

        val settlements = mutableListOf<Settlement>()

        // Separate debtors (negative balance) and creditors (positive balance)
        repeat(members.size * 2) {
            val debtors = balances.entries
                .filter { it.value < BigDecimal.ZERO }
                .sortedBy { it.value } // Most negative first

            val creditors = balances.entries
                .filter { it.value > BigDecimal.ZERO }
                .sortedByDescending { it.value } // Most positive first

            if (debtors.isEmpty() || creditors.isEmpty()) return@repeat

            val debtor = debtors.first()
            val creditor = creditors.first()

            val debtAmount = debtor.value.abs()
            val creditAmount = creditor.value

            val transferAmount = minOf(debtAmount, creditAmount)

            if (transferAmount > BigDecimal("0.01")) {
                val debtorMember = memberMap[debtor.key]
                val creditorMember = memberMap[creditor.key]

                if (debtorMember != null && creditorMember != null) {
                    settlements.add(
                        Settlement(
                            fromMemberId = debtor.key,
                            fromMemberName = debtorMember.name,
                            toMemberId = creditor.key,
                            toMemberName = creditorMember.name,
                            amount = transferAmount.setScale(2, RoundingMode.HALF_UP).toDouble()
                        )
                    )
                }

                balances[debtor.key] = debtor.value.add(transferAmount)
                balances[creditor.key] = creditor.value.subtract(transferAmount)
            }
        }

        return TripReport(settlements = settlements, memberSummaries = memberSummaries)
    }
}
