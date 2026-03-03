package com.splittrip.data.repository
import com.splittrip.data.dao.*
import com.splittrip.data.entities.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

data class Settlement(val fromMemberId: String, val fromMemberName: String, val toMemberId: String, val toMemberName: String, val amount: Double)
data class MemberSummary(val member: Member, val totalPaid: Double, val totalConsumed: Double, val balance: Double)
data class TripReport(val settlements: List<Settlement>, val memberSummaries: List<MemberSummary>)

@Singleton
class SplitTripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao
) {
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()
    suspend fun getTripById(tripId: String): Trip? = tripDao.getTripById(tripId)
    suspend fun createTrip(trip: Trip) = tripDao.insertTrip(trip)
    suspend fun deleteTrip(tripId: String) {
        tripDao.deleteTripById(tripId)
        expenseDao.deleteExpensesByTrip(tripId)
        memberDao.deleteMembersByTrip(tripId)
    }
    fun getMembersByTrip(tripId: String): Flow<List<Member>> = memberDao.getMembersByTrip(tripId)
    suspend fun addMember(member: Member) = memberDao.insertMember(member)
    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
        recalculateBalances(member.tripId)
    }
    fun getExpensesByTrip(tripId: String): Flow<List<Expense>> = expenseDao.getExpensesByTrip(tripId)
    suspend fun addExpense(expense: Expense) { expenseDao.insertExpense(expense); recalculateBalances(expense.tripId) }
    suspend fun updateExpense(expense: Expense) { expenseDao.updateExpense(expense); recalculateBalances(expense.tripId) }
    suspend fun deleteExpense(expense: Expense) { expenseDao.deleteExpense(expense); recalculateBalances(expense.tripId) }

    suspend fun recalculateBalances(tripId: String) {
        val members = memberDao.getMembersByTripSync(tripId)
        val expenses = expenseDao.getExpensesByTripSync(tripId)
        val balanceMap = members.associate { it.memberId to BigDecimal.ZERO }.toMutableMap()
        for (expense in expenses) {
            if (expense.participants.isEmpty()) continue
            val share = BigDecimal(expense.totalAmount).divide(BigDecimal(expense.participants.size), 10, RoundingMode.HALF_UP)
            for ((memberId, amountPaid) in expense.paidBy) balanceMap[memberId] = (balanceMap[memberId] ?: BigDecimal.ZERO).add(BigDecimal(amountPaid))
            for (memberId in expense.participants) balanceMap[memberId] = (balanceMap[memberId] ?: BigDecimal.ZERO).subtract(share)
        }
        for (member in members) {
            val balance = balanceMap[member.memberId] ?: BigDecimal.ZERO
            memberDao.updateBalance(member.memberId, balance.setScale(2, RoundingMode.HALF_UP).toDouble())
        }
    }

    suspend fun generateSettlements(tripId: String): TripReport {
        val members = memberDao.getMembersByTripSync(tripId)
        val expenses = expenseDao.getExpensesByTripSync(tripId)
        val totalPaidMap = members.associate { it.memberId to 0.0 }.toMutableMap()
        val totalConsumedMap = members.associate { it.memberId to 0.0 }.toMutableMap()
        for (expense in expenses) {
            if (expense.participants.isEmpty()) continue
            val share = expense.totalAmount / expense.participants.size
            for ((id, amt) in expense.paidBy) totalPaidMap[id] = (totalPaidMap[id] ?: 0.0) + amt
            for (id in expense.participants) totalConsumedMap[id] = (totalConsumedMap[id] ?: 0.0) + share
        }
        val memberSummaries = members.map { m -> MemberSummary(m, totalPaidMap[m.memberId] ?: 0.0, totalConsumedMap[m.memberId] ?: 0.0, m.totalBalance) }
        val memberMap = members.associateBy { it.memberId }
        val balances = members.associate { it.memberId to BigDecimal(it.totalBalance).setScale(2, RoundingMode.HALF_UP) }.toMutableMap()
        val settlements = mutableListOf<Settlement>()
        repeat(members.size * 2) {
            val debtors = balances.entries.filter { it.value < BigDecimal.ZERO }.sortedBy { it.value }
            val creditors = balances.entries.filter { it.value > BigDecimal.ZERO }.sortedByDescending { it.value }
            if (debtors.isEmpty() || creditors.isEmpty()) return@repeat
            val debtor = debtors.first(); val creditor = creditors.first()
            val transfer = minOf(debtor.value.abs(), creditor.value)
            if (transfer > BigDecimal("0.01")) {
                val dm = memberMap[debtor.key]; val cm = memberMap[creditor.key]
                if (dm != null && cm != null) settlements.add(Settlement(debtor.key, dm.name, creditor.key, cm.name, transfer.setScale(2, RoundingMode.HALF_UP).toDouble()))
                balances[debtor.key] = debtor.value.add(transfer)
                balances[creditor.key] = creditor.value.subtract(transfer)
            }
        }
        return TripReport(settlements, memberSummaries)
    }
}
