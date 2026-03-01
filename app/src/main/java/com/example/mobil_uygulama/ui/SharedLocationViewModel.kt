package com.example.mobil_uygulama.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class DetectionItem(
    val label: String,
    val confidence: Double,
    val imageBase64: String
)

class SharedLocationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("km_data", Context.MODE_PRIVATE)

    val speedKmh = MutableLiveData<Float>().apply { value = 0f }
    private val distances = MutableLiveData<MutableMap<String, Float>>()

    // Tanınan trafik levhası
    val lastDetection = MutableLiveData<DetectionItem>()

    // Dashboard istatistikleri için yeni eklediğimiz alanlar
    private val _totalSigns = MutableLiveData<Int>().apply { value = 0 }
    val totalSigns: LiveData<Int> = _totalSigns

    private val _avgConfidence = MutableLiveData<Double>().apply { value = 0.0 }
    val avgConfidence: LiveData<Double> = _avgConfidence

    private var cumulativeConfidence = 0.0
    private var signCount = 0

    init {
        distances.value = loadDistances()
        checkWeeklyReset()
    }

    fun updateSpeed(speed: Float) {
        speedKmh.value = speed
        // Speed MS'ye çevriliyor, ancak mesafeyi 2 saniyelik interval üzerinden kabaca hesaplıyorsun.
        // İleride daha hassas bir konum-mesafe algoritmasına geçmelisin (örn: Location.distanceTo)
        val speedMs = speed / 3.6f
        val distance = speedMs * 2

        val day = getCurrentDayCode()
        val map = distances.value ?: getEmptyWeekMap()

        map[day] = (map[day] ?: 0f) + (distance / 1000f)

        distances.value = map
        saveDistances(map)
    }

    fun setDetectedItem(label: String, confidence: Double, imageBase64: String) {
        lastDetection.postValue(DetectionItem(label, confidence, imageBase64)) // postValue arkaplanda güvenlidir
        updateStatistics(confidence)
    }

    private fun updateStatistics(newConfidence: Double) {
        signCount++
        cumulativeConfidence += newConfidence

        _totalSigns.postValue(signCount)
        _avgConfidence.postValue(cumulativeConfidence / signCount)
    }

    fun getDistances(): LiveData<MutableMap<String, Float>> = distances

    // KRİTİK DÜZELTME: Locale.US kullanarak sistem dilinden bağımsız standart "Mon, Tue" formatı sağlandı
    private fun getCurrentDayCode(): String {
        val sdf = SimpleDateFormat("EEE", Locale.US)
        return sdf.format(Date())
    }

    private fun saveDistances(map: MutableMap<String, Float>) {
        val json = JSONObject(map as Map<*, *>).toString()
        prefs.edit().putString("daily_km", json).apply()
    }

    private fun loadDistances(): MutableMap<String, Float> {
        val jsonStr = prefs.getString("daily_km", null) ?: return getEmptyWeekMap()
        val json = JSONObject(jsonStr)
        val map = getEmptyWeekMap() // Tüm günlerin var olduğundan emin ol
        for (key in json.keys()) {
            map[key] = json.getDouble(key).toFloat()
        }
        return map
    }

    private fun checkWeeklyReset() {
        val currentDay = getCurrentDayCode()
        val storedResetDay = prefs.getString("last_reset_day", null)

        if (currentDay == "Mon" && storedResetDay != "Mon") {
            val resetMap = getEmptyWeekMap()
            distances.value = resetMap
            saveDistances(resetMap)
            prefs.edit().putString("last_reset_day", "Mon").apply()
        } else if (currentDay != "Mon") {
            prefs.edit().putString("last_reset_day", currentDay).apply()
        }
    }

    private fun getEmptyWeekMap(): MutableMap<String, Float> {
        return mutableMapOf(
            "Mon" to 0f, "Tue" to 0f, "Wed" to 0f,
            "Thu" to 0f, "Fri" to 0f, "Sat" to 0f, "Sun" to 0f
        )
    }
}