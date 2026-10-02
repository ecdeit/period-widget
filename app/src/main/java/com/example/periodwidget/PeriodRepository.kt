package com.example.periodwidget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PeriodRepository(private val dao: PeriodDao) {

    fun observePeriods(): LiveData<List<PeriodEntity>> = dao.observePeriods().asLiveData()

    suspend fun periodsOnce(): List<PeriodEntity> = dao.getPeriodsOnce()

    suspend fun addPeriod(startDate: String, length: Int) {
        dao.insertPeriod(PeriodEntity(startDate = startDate, length = length))
    }

    suspend fun removePeriod(startDate: String) {
        dao.deletePeriod(startDate)
    }

    suspend fun clearAll() {
        dao.clearAllPeriods()
    }
}

class PeriodViewModel(private val repository: PeriodRepository) : ViewModel() {

    private val _periods = MutableLiveData<List<PeriodEntity>>()
    val periods: LiveData<List<PeriodEntity>> = _periods

    private val _stats = MutableLiveData<CycleStats>()
    val stats: LiveData<CycleStats> = _stats

    private val _todayStatus = MutableLiveData<String>()
    val todayStatus: LiveData<String> = _todayStatus

    init {
        viewModelScope.launch {
            repository.observePeriods().observeForever { list ->
                if (list != null) {
                    val computed = CycleCalculator.computeStats(list)
                    _periods.postValue(list)
                    _stats.postValue(computed)
                    _todayStatus.postValue(CycleCalculator.describeToday(list, computed))
                }
            }
        }
    }

    fun addPeriod(startDate: String, length: Int) {
        viewModelScope.launch { repository.addPeriod(startDate, length) }
    }

    fun removePeriod(startDate: String) {
        viewModelScope.launch { repository.removePeriod(startDate) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun calendar(year: Int, month: Int): List<List<DayStatus?>> {
        val list = _periods.value ?: emptyList()
        val current = _stats.value ?: CycleStats()
        return CycleCalculator.generateCalendarMatrix(year, month, list, current)
    }

    /** Factory so the Activity can hand the repository in. */
    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PeriodViewModel(repository) as T
        }
    }
}
