package com.example.mobil_uygulama.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    // İleride veritabanından (Room/SQLite) çekilecek özet veriler için hazır yapı
    private val _totalSigns = MutableLiveData<Int>().apply { value = 0 }
    val totalSigns: LiveData<Int> = _totalSigns

    private val _avgConfidence = MutableLiveData<Double>().apply { value = 0.0 }
    val avgConfidence: LiveData<Double> = _avgConfidence

    // Örnek: Yeni levha eklendikçe istatistikleri güncellemek için bir fonksiyon
    fun updateStatistics(total: Int, averageConf: Double) {
        _totalSigns.value = total
        _avgConfidence.value = averageConf
    }
}