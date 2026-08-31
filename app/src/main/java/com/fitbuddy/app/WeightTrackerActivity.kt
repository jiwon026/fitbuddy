package com.fitbuddy.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fitbuddy.app.databinding.ActivityWeightTrackerBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeightTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeightTrackerBinding
    private var currentWeight = 65.0f
    private val weightHistory = mutableListOf<Pair<String, Float>>()

    // 위치 정보 클라이언트
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeightTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val initialWeight = intent.getStringExtra("INITIAL_WEIGHT")?.toFloatOrNull() ?: 65.0f
        currentWeight = initialWeight

        initializeWeightHistory()
        updateUI()
        setupListeners()
        setupChart()
    }

    private fun initializeWeightHistory() {
        weightHistory.clear()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val day = calendar.clone() as Calendar
            day.add(Calendar.DATE, -i)
            val dateString = dateFormat.format(day.time)
            val randomChange = if (i > 0) (-2..2).random() * 0.5f else 0f
            weightHistory.add(dateString to currentWeight + randomChange)
        }
        currentWeight = weightHistory.last().second
    }

    private fun setupListeners() {
        binding.btnAddWeight.setOnClickListener { showWeightInputDialog() }
        binding.cardExercise.setOnClickListener { startActivity(Intent(this, ExerciseCategoryActivity::class.java)) }
        binding.cardChat.setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }
        binding.cardFacilities.setOnClickListener { handleNearbyFacilitiesClick() }
    }

    private fun setupChart() {
        val entries = ArrayList<Entry>()
        weightHistory.forEachIndexed { index, pair ->
            entries.add(Entry(index.toFloat(), pair.second))
        }

        val dataSet = LineDataSet(entries, "체중 변화").apply {
            color = Color.parseColor("#6366F1")
            setCircleColor(Color.parseColor("#6366F1"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        binding.chart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(weightHistory.map { it.first })
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
            }
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun showWeightInputDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_weight_input, null)
        val etWeight = dialogView.findViewById<EditText>(R.id.et_weight)

        builder.setView(dialogView)
            .setTitle("오늘의 체중")
            .setPositiveButton("확인") { _, _ ->
                val weight = etWeight.text.toString().toFloatOrNull()
                if (weight != null) {
                    currentWeight = weight
                    val todayDate = weightHistory.last().first
                    weightHistory[weightHistory.size - 1] = todayDate to weight
                    updateUI()
                    setupChart()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateUI() {
        binding.tvCurrentWeight.text = String.format("%.1fkg", currentWeight)
    }

    // ====================================
    // 위치 정보 관련
    // ====================================

    private fun handleNearbyFacilitiesClick() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 있을 경우, 위치 가져오기
                getLastLocationAndStartActivity()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // (선택사항) 권한이 왜 필요한지 설명하는 UI 표시
                Toast.makeText(this, "주변 시설을 찾으려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // 권한 요청
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 사용자가 권한을 허용한 경우
            getLastLocationAndStartActivity()
        } else {
            // 사용자가 권한을 거부한 경우
            Toast.makeText(this, "위치 권한이 거부되어 기능을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocationAndStartActivity() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val intent = Intent(this, NearbyFacilityActivity::class.java).apply {
                            putExtra("USER_LAT", location.latitude)
                            putExtra("USER_LON", location.longitude)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "현재 위치를 가져올 수 없습니다. 위치 서비스를 확인해주세요.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Log.e("Location", "위치 권한이 없습니다.", e)
        }
    }
}
