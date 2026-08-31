package com.fitbuddy.app

import com.fitbuddy.app.network.ApiClient
import com.fitbuddy.app.network.UserInfoRequest

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fitbuddy.app.databinding.ActivityUserInfoBinding
import kotlinx.coroutines.launch

class UserInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInfoBinding
    private var selectedGender: String = ""
    private var selectedGoal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        // 버튼 초기 상태 업데이트
        updateGenderButtons()
        updateGoalButtons()
    }

    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 성별 선택
        binding.btnMale.setOnClickListener { selectGender("male") }
        binding.btnFemale.setOnClickListener { selectGender("female") }
        binding.btnOther.setOnClickListener { selectGender("other") }

        // 운동 목적 선택
        binding.btnWeightLoss.setOnClickListener { selectGoal("weight-loss") }
        binding.btnMuscleGain.setOnClickListener { selectGoal("muscle-gain") }
        binding.btnFitness.setOnClickListener { selectGoal("fitness") }
        binding.btnMassGain.setOnClickListener { selectGoal("mass-gain") }
        binding.btnBalance.setOnClickListener { selectGoal("balance") }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            val height = binding.etHeight.text.toString().trim()
            val weight = binding.etWeight.text.toString().trim()

            if (!validateInput(height, weight)) return@setOnClickListener

            // 회원가입 화면에서 넘겨준 이메일 가져오기 (SignupActivity에서 putExtra("EMAIL", email) 해줘야 함)
            val email = intent.getStringExtra("EMAIL") ?: run {
                Toast.makeText(this, "이메일 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = UserInfoRequest(
                email = email,
                height_cm = height.toInt(),
                weight_kg = weight.toDouble(),
                gender = selectedGender,
                workout_goal = selectedGoal
            )

            // 서버로 사용자 정보 전송
            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.updateUserInfo(request)

                    if (response.success) {
                        Toast.makeText(
                            this@UserInfoActivity,
                            "정보가 저장되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@UserInfoActivity, WeightTrackerActivity::class.java).apply {
                            putExtra("INITIAL_WEIGHT", weight)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@UserInfoActivity,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@UserInfoActivity,
                        "서버 오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun selectGender(gender: String) {
        // 선택 토글 기능 추가
        selectedGender = if (selectedGender == gender) "" else gender
        updateGenderButtons()
    }

    private fun updateGenderButtons() {
        val activeColor = ContextCompat.getColor(this, R.color.indigo_500)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray_200)

        // backgroundTintList를 사용하여 버튼 스타일 유지
        binding.btnMale.backgroundTintList =
            ColorStateList.valueOf(if (selectedGender == "male") activeColor else inactiveColor)
        binding.btnFemale.backgroundTintList =
            ColorStateList.valueOf(if (selectedGender == "female") activeColor else inactiveColor)
        binding.btnOther.backgroundTintList =
            ColorStateList.valueOf(if (selectedGender == "other") activeColor else inactiveColor)
    }

    private fun selectGoal(goal: String) {
        // 선택 토글 기능 추가
        selectedGoal = if (selectedGoal == goal) "" else goal
        updateGoalButtons()
    }

    private fun updateGoalButtons() {
        val activeColor = ContextCompat.getColor(this, R.color.indigo_500)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray_200)

        // backgroundTintList를 사용하여 버튼 스타일 유지
        binding.btnWeightLoss.backgroundTintList =
            ColorStateList.valueOf(if (selectedGoal == "weight-loss") activeColor else inactiveColor)
        binding.btnMuscleGain.backgroundTintList =
            ColorStateList.valueOf(if (selectedGoal == "muscle-gain") activeColor else inactiveColor)
        binding.btnFitness.backgroundTintList =
            ColorStateList.valueOf(if (selectedGoal == "fitness") activeColor else inactiveColor)
        binding.btnMassGain.backgroundTintList =
            ColorStateList.valueOf(if (selectedGoal == "mass-gain") activeColor else inactiveColor)
        binding.btnBalance.backgroundTintList =
            ColorStateList.valueOf(if (selectedGoal == "balance") activeColor else inactiveColor)
    }

    private fun validateInput(height: String, weight: String): Boolean {
        if (height.isEmpty()) {
            Toast.makeText(this, "키를 입력해주세요", Toast.LENGTH_SHORT).show()
            binding.etHeight.requestFocus()
            return false
        }

        if (weight.isEmpty()) {
            Toast.makeText(this, "몸무게를 입력해주세요", Toast.LENGTH_SHORT).show()
            binding.etWeight.requestFocus()
            return false
        }

        if (selectedGender.isEmpty()) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedGoal.isEmpty()) {
            Toast.makeText(this, "운동 목적을 선택해주세요", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}
