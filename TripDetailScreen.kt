package com.splittrip.di
import android.content.Context
import androidx.room.Room
import com.splittrip.data.dao.*
import com.splittrip.data.database.SplitTripDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SplitTripDatabase =
        Room.databaseBuilder(context, SplitTripDatabase::class.java, "splittrip_database").build()
    @Provides fun provideTripDao(db: SplitTripDatabase): TripDao = db.tripDao()
    @Provides fun provideMemberDao(db: SplitTripDatabase): MemberDao = db.memberDao()
    @Provides fun provideExpenseDao(db: SplitTripDatabase): ExpenseDao = db.expenseDao()
}
