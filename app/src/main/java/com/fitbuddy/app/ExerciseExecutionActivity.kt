package com.fitbuddy.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fitbuddy.app.databinding.ActivityExerciseExecutionBinding
import com.fitbuddy.app.network.ApiClient
import com.fitbuddy.app.network.PoseImageRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Matrix
import kotlinx.coroutines.*

class ExerciseExecutionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseExecutionBinding
    private lateinit var cameraExecutor: ExecutorService

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var isRunning = false

    private val poseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 포즈 분석 관련
    private var lastPoseSentAt: Long = 0L
    private val poseIntervalMs: Long = 700L   // 0.7초마다 한 번씩 서버로 프레임 전송

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseExecutionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.poseOverlay.setIsFrontCamera(true)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 카메라 권한 확인
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }

        setupUI()
        setupListeners()
        startTimer()
    }

    private fun setupUI() {
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "운동"
        val exerciseDuration = intent.getStringExtra("EXERCISE_DURATION") ?: "30초"
        binding.tvExerciseName.text = exerciseName

        val seconds = exerciseDuration.replace("초", "").trim().toIntOrNull() ?: 30
        timeLeftInMillis = seconds * 1000L
        updateTimerText()

        // 초기 피드백
        binding.tvFeedback.text = "카메라를 정면으로 바라봐 주세요 👀"
    }

    private fun setupListeners() {
        binding.btnPause.setOnClickListener {
            if (isRunning) {
                pauseTimer()
                binding.btnPause.text = "계속하기"
            } else {
                resumeTimer()
                binding.btnPause.text = "일시정지"
            }
        }

        binding.btnFinish.setOnClickListener {
            Toast.makeText(this, "운동 완료! 수고하셨습니다! 💪", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ============================
    // CameraX 설정 (Preview + 분석)
    // ============================
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            var selector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    selector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            } catch (e: Exception) {
                selector = CameraSelector.DEFAULT_BACK_CAMERA
            }

            // 분석용 ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageForPose(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, selector, preview, imageAnalyzer // cameraSelector -> selector 로 변경
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
                Toast.makeText(this, "카메라를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            finish() // 권한 없으면 액티비티 종료
        }
    }

    // ============================
    // 타이머
    // ============================
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
                // 피드백은 서버 응답에서 직접 업데이트하므로, 여기서는 타이머만 업데이트
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                updateTimerText()
                Toast.makeText(this@ExerciseExecutionActivity, "운동 완료! 🎉", Toast.LENGTH_SHORT).show()
                binding.btnFinish.performClick()
            }
        }.start()

        isRunning = true
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
    }

    private fun resumeTimer() {
        startTimer()
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        binding.tvTimer.text = timeFormatted
    }

    // ============================
    // 포즈 분석 → 서버 호출 파트
    // ============================

    // 클래스 상단에 이미 있음:
// private val poseIntervalMs: Long = 700L

    // ✅ 동시에 여러 요청이 겹치지 않게 막기 위한 플래그
    @Volatile
    private var isPoseRequestRunning: Boolean = false

    private fun processImageForPose(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()

        // 너무 자주 보내지 않도록 (0.7초 간격)
        if (now - lastPoseSentAt < poseIntervalMs) {
            imageProxy.close()
            return
        }
        lastPoseSentAt = now

        // 이미 하나 보내는 중이면 이번 프레임은 버리기
        if (isPoseRequestRunning) {
            imageProxy.close()
            return
        }
        isPoseRequestRunning = true

        // ImageProxy → Bitmap
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        // ✅ poseScope 사용 (lifecycleScope 말고)
        poseScope.launch {
            try {
                val base64 = bitmap.toBase64()

                Log.d("POSE_API", "send frame to server")

                val res = ApiClient.api.analyzePose(
                    PoseImageRequest(image_base64 = base64)
                )

                Log.d(
                    "POSE_API",
                    "recv: knee=${res.knee_angle}, hip=${res.hip_angle}, points=${res.keypoints?.size ?: 0}"
                )

                withContext(Dispatchers.Main) {
                    binding.tvFeedback.text = res.feedback

                    val pts = res.keypoints
                    if (pts != null && pts.isNotEmpty()) {
                        Log.d("POSE_OVERLAY", "draw points: ${pts.size}")
                        binding.poseOverlay.updatePose(pts)
                    } else {
                        Log.d("POSE_OVERLAY", "no points received")
                    }
                }

            } catch (e: Exception) {
                // ✅ 여기서 진짜 에러를 보고 싶음 (취소든 뭐든 다 찍기)
                Log.e("POSE_API", "Error in pose request", e)
            } finally {
                isPoseRequestRunning = false
            }
        }
    }



    // ImageProxy → Bitmap 변환
    // ImageProxy → Bitmap 변환 (회전 보정 포함)
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // ★ sensor 기준으로 만들어진 원본 비트맵
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 75, out)
        val jpegBytes = out.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // ★ CameraX가 알려주는 회전 각도만큼 돌려서 “화면이랑 같은 방향”으로 맞추기
        val rotationDegrees = imageInfo.rotationDegrees.toFloat()
        if (rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }

        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }


    // Bitmap → Base64 변환
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        cameraExecutor.shutdown()
        poseScope.cancel()   // ✅ 추가
    }

}
