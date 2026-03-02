package com.splittrip.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splittrip.data.entities.Trip
import com.splittrip.data.repository.SplitTripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripWithBalance(
    val trip: Trip,
    val myBalance: Double = 0.0,
    val memberCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SplitTripRepository
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = repository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
        }
    }
}
