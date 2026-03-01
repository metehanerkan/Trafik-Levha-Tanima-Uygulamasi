package com.example.mobil_uygulama.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mobil_uygulama.R
import com.example.mobil_uygulama.databinding.FragmentMapsBinding
import com.example.mobil_uygulama.ui.SharedLocationViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.gson.JsonParser
import com.google.maps.android.PolyUtil
import okhttp3.*
import java.io.IOException

class MapsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val sharedLocationViewModel: SharedLocationViewModel by activityViewModels()

    private var currentPolyline: Polyline? = null

    // UYARI: Bunu ileride local.properties dosyasına taşı!
    private val placesApiKey = "AIzaSyDeD7flxa08FC6BAtHjz0c2amIa-upMgok"

    // Modern Intent Başlatıcı (startActivityForResult yerine)
    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val destinationLatLng = place.latLng

            if (destinationLatLng != null) {
                drawRouteToDestination(destinationLatLng, place.name, place.address)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, placesApiKey)
        }

        setupUI()
        loadMap()
        observeSharedData()
    }

    private fun setupUI() {
        // Arama Butonu
        binding.btnSearchPlace.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            autocompleteLauncher.launch(intent)
        }

        // Konumuma Git Butonu (Kendi tasarladığımız FAB)
        binding.btnCenterLocation.setOnClickListener {
            centerCameraOnUser()
        }
    }

    private fun observeSharedData() {
        // Hız verisini merkezi ViewModel'den dinle ve modern UI'a bas
        sharedLocationViewModel.speedKmh.observe(viewLifecycleOwner) { speed ->
            binding.txtSpeed.text = "%.1f km/h".format(speed)
        }
    }

    private fun loadMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (hasLocationPermission()) {
            // Google Maps'in kendi mavi nokta ve konum takip sistemini açıyoruz.
            // Böylece saniyede bir map.clear() yapıp rotayı silme saçmalığından kurtuluyoruz!
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false // Kendi FAB'ımızı kullanacağımız için varsayılanı gizledik

            centerCameraOnUser()
        }
    }

    @SuppressLint("MissingPermission")
    private fun centerCameraOnUser() {
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                } ?: Toast.makeText(requireContext(), "Konum aranıyor...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun drawRouteToDestination(destinationLatLng: LatLng, placeName: String?, placeAddress: String?) {
        if (!hasLocationPermission()) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val originLatLng = LatLng(it.latitude, it.longitude)

                // Haritayı hedefe odaklar ve işaretçi koyar
                googleMap?.clear() // Sadece yeni rota çizilirken eski rotayı ve markerları temizle
                googleMap?.addMarker(MarkerOptions().position(destinationLatLng).title(placeName))
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15f))

                fetchRouteFromAPI(originLatLng, destinationLatLng, placeName, placeAddress)
            }
        }
    }

    private fun fetchRouteFromAPI(origin: LatLng, destination: LatLng, placeName: String?, placeAddress: String?) {
        binding.txtDistanceDuration.text = "Rota Hesaplanıyor..."

        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$placesApiKey"
        val request = Request.Builder().url(url).build()

        // Modern Asenkron Ağ İsteği (Ana thread'i kilitlemez)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Rota alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.txtDistanceDuration.text = "Rota çizilemedi."
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JsonParser.parseString(body).asJsonObject
                        val routes = json.getAsJsonArray("routes")

                        if (routes.size() > 0) {
                            val route = routes[0].asJsonObject
                            val overviewPolyline = route.getAsJsonObject("overview_polyline").get("points").asString
                            val leg = route.getAsJsonArray("legs")[0].asJsonObject
                            val distance = leg.getAsJsonObject("distance").get("text").asString
                            val duration = leg.getAsJsonObject("duration").get("text").asString
                            val polylineList = PolyUtil.decode(overviewPolyline)

                            requireActivity().runOnUiThread {
                                currentPolyline?.remove()
                                currentPolyline = googleMap?.addPolyline(
                                    PolylineOptions()
                                        .addAll(polylineList)
                                        .color(getThemeColor(com.google.android.material.R.attr.colorPrimary)) // Temaya uygun renk
                                        .width(14f)
                                        .geodesic(true)
                                )

                                val shortAddress = if ((placeAddress ?: "").length > 35) placeAddress?.substring(0, 35) + "..." else placeAddress
                                binding.txtDistanceDuration.text = "Varış: $duration ($distance)\n$shortAddress"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MapsFragment", "JSON Parse Error: ${e.message}")
                    }
                }
            }
        })
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Temadan renk çeken yardımcı fonksiyon (Gece/Gündüz modu için)
    private fun getThemeColor(attrId: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Bellek sızıntısını önler
    }
}