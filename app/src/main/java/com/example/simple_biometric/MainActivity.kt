package com.example.simple_biometric

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.simple_biometric.databinding.ActivityMainBinding
import java.util.concurrent.Executor
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding

    companion object {
        const val TAG: String = "MainActivity"
    }
    private var executor: Executor? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "registerFroActivityResult - result : $result")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "registerForActivityResult - RESULT_OK")
            authenticateToEncrypt() // 생체 인증 가능 여부 확인 다시 호출
        } else {
            Log.d(TAG, "registerForActivityResult - NOT RESULT_OK")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricPrompt = setBiometricPrompt()
        promptInfo = setPromptInfo()

        // 지문 인증 호출 버튼 클릭 시
        binding.btnBioSet.setOnClickListener {
            authenticateToEncrypt()
        }
    }

    private fun setPromptInfo() : BiometricPrompt.PromptInfo {
        val promptBuilder: BiometricPrompt.PromptInfo.Builder = BiometricPrompt.PromptInfo.Builder()

        promptBuilder.setTitle("Biometric login for my app")
        promptBuilder.setSubtitle("Log in using your biometric credential")
        promptBuilder.setNegativeButtonText("Use account password")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 안면인식 ap사용 android 11부터 지원
            promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }
        promptInfo = promptBuilder.build()
        return promptInfo as BiometricPrompt.PromptInfo
    }

    private fun setBiometricPrompt() : BiometricPrompt {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this@MainActivity, executor!!, object  : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, """지문 인식 ERROR [errorCode: $errorCode, errString: $errString]""".trimIndent(), Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(this@MainActivity, "지문 인식 성공", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "지문 인식 실패", Toast.LENGTH_SHORT).show()
            }
        })
        return biometricPrompt as BiometricPrompt
    }

    // 생체 인식 인증을 사용할 수 있는지 확인
    fun authenticateToEncrypt() = with(binding) {
        Log.d(TAG, "authenticateToEncrypt()")
        var textStatus = ""
        val biometricManager = BiometricManager.from(this@MainActivity)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            // 생체 인증 가능
            BiometricManager.BIOMETRIC_SUCCESS -> textStatus = "App can authenticate using biometrics."

            // 기기에서 생체 인증을 지원하지 않는 경우
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> textStatus = "No biometric features available on this device."

            // 현재 생체 인증을 사용할 수 없는 경우
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> textStatus = "Biometric features are currently unavailable."

            // 생체 인식 정보가 등록되어 있지 않은 경우
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                textStatus = "Prompts the user to create credentials that your app accepts."
                val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                dialogBuilder.setTitle("나의앱").setMessage("지문 등록이 필요하여 설정으로 이동합니다")
                    .setPositiveButton("확인") { _, _ ->
                        goBiometricSettings()
                    }
                    .setNegativeButton("취소") { dialog, _ ->
                        dialog.cancel()
                    }
                dialogBuilder.show()
            }
            // 기타 실패
            else -> textStatus = "Fail Biometric facility"
        }
        binding.tvStatus.text = textStatus

        // 인증 실행하기
        goAuthenticate()
    }

    // 생체 인식 인증 실행
    private fun goAuthenticate() {
        Log.d(TAG, "goAuthenticate - promptInfo : $promptInfo")
        promptInfo?.let {
            // 인증 실행
            biometricPrompt?.authenticate(it)
        }
    }

    // 지문 등록 화면으로 이동
    fun goBiometricSettings() {
        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }
        } else {
            Intent(Settings.ACTION_FINGERPRINT_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }
        }
        loginLauncher.launch(enrollIntent)
    }
}