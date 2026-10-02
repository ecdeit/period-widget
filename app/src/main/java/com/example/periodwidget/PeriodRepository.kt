package com.example.periodwidget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data access plus the observable state the UI renders.
 *
 * Without Room there is no Flow to collect, so the state is reloaded explicitly
 * after every write. One table and one screen makes that the simpler contract.
 */
class PeriodRepository(private val store: PeriodStore) {

    suspend fun all(): List<PeriodEntity> = withContext(Dispatchers.IO) { store.all() }

    suspend fun add(startDate: String, length: Int) =
        withContext(Dispatchers.IO) { store.upsert(startDate, length) }

    suspend fun remove(startDate: String) =
        withContext(Dispatchers.IO) { store.delete(startDate) }

    suspend fun clear() = withContext(Dispatchers.IO) { store.clear() }
}

class PeriodViewModel(private val repository: PeriodRepository) : ViewModel() {

    private val _periods = MutableLiveData<List<PeriodEntity>>(emptyList())
    val periods: LiveData<List<PeriodEntity>> = _periods

    private val _stats = MutableLiveData(CycleStats())
    val stats: LiveData<CycleStats> = _stats

    private val _todayStatus = MutableLiveData("正在读取…")
    val todayStatus: LiveData<String> = _todayStatus

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val list = repository.all()
            val computed = CycleCalculator.computeStats(list)
            _periods.value = list
            _stats.value = computed
            _todayStatus.value = CycleCalculator.describeToday(list, computed)
        }
    }

    fun addPeriod(startDate: String, length: Int) {
        viewModelScope.launch {
            repository.add(startDate, length)
            refresh()
        }
    }

    fun removePeriod(startDate: String) {
        viewModelScope.launch {
            repository.remove(startDate)
            refresh()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clear()
            refresh()
        }
    }

    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PeriodViewModel(repository) as T
        }
    }
}
