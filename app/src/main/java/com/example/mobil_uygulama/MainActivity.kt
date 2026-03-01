package com.example.mobil_uygulama

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.mobil_uygulama.databinding.ActivityMainBinding
import com.example.mobil_uygulama.ui.SharedLocationViewModel
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import org.json.JSONObject
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeech
    private lateinit var webSocket: WebSocket

    // Konum değişkenleri eklendi
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val sharedLocationViewModel: SharedLocationViewModel by viewModels()

    // Modern İzin İsteme Yöntemi
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                startLocationUpdates()
            }
            else -> {
                Toast.makeText(this, "Hız ve mesafe takibi için konum izni gereklidir.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Action bar'ı temadan kaldırdığımız için setupActionBarWithNavController sildik.
        val navView: BottomNavigationView = binding.navView

// NavHostFragment'i SupportFragmentManager ile buluyoruz
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController

// Ve menüye bağlıyoruz
        navView.setupWithNavController(navController)

        tts = TextToSpeech(this, this)

        // Konum Servisini Hazırla
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        checkLocationPermissions()

        initWebSocket()
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            startLocationUpdates()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->

                    // 1. Hassasiyet (Accuracy) Kontrolü
                    // Eğer GPS'in hata payı 30 metreden fazlaysa, bu veriye güvenme ve atla.
                    if (location.hasAccuracy() && location.accuracy > 30f) {
                        return@let
                    }

                    // 2. Hızı km/h cinsine çevir
                    var speedKmh = location.speed * 3.6f

                    // 3. Gürültü Filtresi (Noise Filter)
                    // İnsan yürüme hızı yaklaşık 4-5 km/h'dir. Araç içindeki ufak tefek
                    // oynamaları veya telefon sabitken olan GPS sapmalarını sıfırla.
                    if (speedKmh < 3.5f) {
                        speedKmh = 0f
                    }

                    sharedLocationViewModel.updateSpeed(speedKmh)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun initWebSocket() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", "192.168.43.79")
        val url = "ws://$ip:8080"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val label = json.getString("label")
                    val confidence = json.getDouble("confidence")
                    val image = json.getString("image")

                    // UI'ı kitlememek için sadece LiveData'yı güncelliyoruz (postValue ile)
                    sharedLocationViewModel.setDetectedItem(label, confidence, image)

                    // Seslendirme işlemini Arka Plana (IO Thread) atıyoruz
                    CoroutineScope(Dispatchers.IO).launch {
                        speakTrafficWarning(label)
                    }

                } catch (e: Exception) {
                    Log.e("WebSocket", "Hata: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "WebSocket bağlantı hatası: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("tr", "TR")
        }
    }

    // Trafik levhasına göre sesli uyarı ver
    private fun speakTrafficWarning(signCode: String) {
        val message = when (signCode.lowercase(Locale.ROOT)) {
            "yesil isik" -> "Yeşil ışık geçebilirsiniz"
            "kirmizi isik" -> "Kırmızı ışık lütfen durun"
            // ... (Diğer tüm when durumlarını aynı şekilde buraya koy)
            else -> "Bilinmeyen trafik levhası"
        }

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Konum dinlemeyi durdur (Pil tasarrufu)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}