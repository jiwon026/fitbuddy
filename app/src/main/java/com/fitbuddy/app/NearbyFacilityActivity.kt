package com.fitbuddy.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitbuddy.app.adapters.FacilityAdapter
import com.fitbuddy.app.databinding.ActivityNearbyFacilityBinding
import com.fitbuddy.app.network.ApiClient
import com.fitbuddy.app.network.FacilityDto
import com.fitbuddy.app.network.FacilityRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class NearbyFacilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearbyFacilityBinding
    private lateinit var adapter: FacilityAdapter
    private val facilityList = mutableListOf<FacilityDto>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearbyFacilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        setupRecyclerView()
        setupWebView()
        setupListeners()

        // 인텐트에서 전달받은 초기 위도/경도 설정
        val lat = intent.getDoubleExtra("USER_LAT", 37.5665) // 기본값: 서울
        val lon = intent.getDoubleExtra("USER_LON", 126.9780)
        updateCoordinates(lat, lon)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = FacilityAdapter(facilityList) { facility ->
            // 아이템 클릭 시 지도 이동
            binding.webviewMap.loadUrl("javascript:moveMap(${facility.lat}, ${facility.lon})")
        }
        binding.rvFacilities.layoutManager = LinearLayoutManager(this)
        binding.rvFacilities.adapter = adapter
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webviewMap.settings.javaScriptEnabled = true
        binding.webviewMap.webViewClient = WebViewClient()
        binding.webviewMap.addJavascriptInterface(WebAppInterface(), "Android")
        loadMapHtml()
    }

    private fun setupListeners() {
        binding.btnCurrentLocation.setOnClickListener { handleCurrentLocationClick() }
        binding.btnSearch.setOnClickListener { searchFacilities() }
    }

    private fun updateCoordinates(lat: Double, lon: Double) {
        binding.etLatitude.setText(lat.toString())
        binding.etLongitude.setText(lon.toString())
        binding.webviewMap.loadUrl("javascript:moveMap($lat, $lon)")
    }

    private fun searchFacilities() {
        val lat = binding.etLatitude.text.toString().toDoubleOrNull()
        val lon = binding.etLongitude.text.toString().toDoubleOrNull()
        val radius = binding.etRadius.text.toString().toDoubleOrNull()

        if (lat == null || lon == null || radius == null) {
            Toast.makeText(this, "유효한 위도, 경도, 반경을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.rvFacilities.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val res = ApiClient.api.getNearbyFacilities(
                    FacilityRequest(user_lat = lat, user_lon = lon, radius_km = radius)
                )

                binding.progressBar.visibility = View.GONE

                if (res.isNotEmpty()) {
                    facilityList.clear()
                    facilityList.addAll(res)
                    adapter.notifyDataSetChanged()
                    binding.rvFacilities.visibility = View.VISIBLE

                    // 지도에 마커 추가
                    binding.webviewMap.loadUrl("javascript:clearMarkers()")
                    res.forEach { facility ->
                        binding.webviewMap.loadUrl("javascript:addMarker(${facility.lat}, ${facility.lon}, \"${facility.name}\")")
                    }

                } else {
                    binding.tvEmpty.text = "주변에 등록된 운동 시설이 없습니다 😢"
                    binding.tvEmpty.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("NearbyFacility", "Error: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.text = "데이터를 불러오는 데 실패했습니다: ${e.message}"
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun loadMapHtml() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>FitBuddy Map</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                <style> html, body, #map { height: 100%; margin: 0; padding: 0; } </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([37.5665, 126.9780], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    var markers = [];

                    function moveMap(lat, lon) {
                        map.setView([lat, lon], 14);
                    }

                    function addMarker(lat, lon, title) {
                        var marker = L.marker([lat, lon]).addTo(map).bindPopup(title);
                        markers.push(marker);
                    }

                    function clearMarkers() {
                        for(var i=0; i<markers.length; i++) {
                            map.removeLayer(markers[i]);
                        }
                        markers = [];
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        binding.webviewMap.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    // ====================================
    // 위치 정보 관련
    // ====================================

    private fun handleCurrentLocationClick() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocationAndStartActivity()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocationAndStartActivity()
        } else {
            Toast.makeText(this, "위치 권한이 거부되어 현재 위치를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocationAndStartActivity() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    updateCoordinates(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("Location", "위치 권한이 없습니다.", e)
        }
    }

    // 웹뷰에서 안드로이드 호출을 위한 인터페이스
    inner class WebAppInterface {
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(this@NearbyFacilityActivity, toast, Toast.LENGTH_SHORT).show()
        }
    }
}
