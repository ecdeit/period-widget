package com.example.periodwidget

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate

class PeriodRepository(private val dao: PeriodDao) {

    suspend fun addPeriod(startDate: String, length: Int) {
        val entity = PeriodEntity(startDate = startDate, length = length)
        dao.insertPeriod(entity)
    }

    suspend fun removePeriod(startDate: String) {
        dao.deletePeriod(startDate)
    }

    suspend fun clearAll() {
        dao.clearAllPeriods()
    }

    fun observeAllPeriods(): LiveData<List<PeriodEntity>> = dao.getAllPeriods().asLiveData()

    suspend fun getAllPeriods(): List<PeriodEntity> = dao.getAllPeriods().first()

    suspend fun getLatestPeriod(): PeriodEntity? = dao.getLatestPeriod()

    suspend fun getSettings(): SettingsEntity {
        return dao.getSettings() ?: SettingsEntity().also { dao.insertSettings(it) }
    }

    suspend fun updateSettings(settings: SettingsEntity) {
        dao.insertSettings(settings)
    }
}

class PeriodViewModel(repository: PeriodRepository) : ViewModel() {

    private val _periods = MutableLiveData<List<PeriodEntity>>()
    val periods: LiveData<List<PeriodEntity>> = _periods

    private val _stats = MutableLiveData<CycleStats>()
    val stats: LiveData<CycleStats> = _stats

    private val _todayStatus = MutableLiveData<String>()
    val todayStatus: LiveData<String> = _todayStatus

    init {
        viewModelScope.launch {
            repository.observeAllPeriods().collect { list ->
                _periods.postValue(list)
                val stats = CycleCalculator.computeStats(list)
                _stats.postValue(stats)
                _todayStatus.postValue(CycleCalculator.getTodayStatus(list, stats))
            }
        }
    }

    fun addPeriod(startDate: String, length: Int) {
        viewModelScope.launch {
            repository.addPeriod(startDate, length)
        }
    }

    fun removePeriod(startDate: String) {
        viewModelScope.launch {
            repository.removePeriod(startDate)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun getCalendarMatrix(year: Int, month: Int): List<List<DayStatus?>> {
        val list = _periods.value ?: emptyList()
        val stats = _stats.value ?: CycleStats()
        return CycleCalculator.generateCalendarMatrix(year, month, list, stats)
    }
}