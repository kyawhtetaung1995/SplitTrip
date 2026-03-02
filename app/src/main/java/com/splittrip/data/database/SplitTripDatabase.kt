package com.splittrip.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.splittrip.data.dao.ExpenseDao
import com.splittrip.data.dao.MemberDao
import com.splittrip.data.dao.TripDao
import com.splittrip.data.entities.Expense
import com.splittrip.data.entities.Member
import com.splittrip.data.entities.Trip

@Database(
    entities = [Trip::class, Member::class, Expense::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SplitTripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
}
