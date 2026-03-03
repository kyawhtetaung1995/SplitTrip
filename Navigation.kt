package com.splittrip.data.database
import androidx.room.*
import com.splittrip.data.dao.*
import com.splittrip.data.entities.*

@Database(entities = [Trip::class, Member::class, Expense::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SplitTripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
}
