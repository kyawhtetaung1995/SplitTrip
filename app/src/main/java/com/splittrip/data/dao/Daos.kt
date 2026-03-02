package com.splittrip.data.dao

import androidx.room.*
import com.splittrip.data.entities.Trip
import com.splittrip.data.entities.Member
import com.splittrip.data.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    suspend fun getTripById(tripId: String): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE tripId = :tripId")
    suspend fun deleteTripById(tripId: String)
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name ASC")
    fun getMembersByTrip(tripId: String): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name ASC")
    suspend fun getMembersByTripSync(tripId: String): List<Member>

    @Query("SELECT * FROM members WHERE memberId = :memberId")
    suspend fun getMemberById(memberId: String): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    @Update
    suspend fun updateMember(member: Member)

    @Query("UPDATE members SET totalBalance = :balance WHERE memberId = :memberId")
    suspend fun updateBalance(memberId: String, balance: Double)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM members WHERE tripId = :tripId")
    suspend fun deleteMembersByTrip(tripId: String)

    @Query("UPDATE members SET totalBalance = 0.0 WHERE tripId = :tripId")
    suspend fun resetBalances(tripId: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun getExpensesByTrip(tripId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    suspend fun getExpensesByTripSync(tripId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId")
    suspend fun getExpenseById(expenseId: String): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesByTrip(tripId: String)
}
