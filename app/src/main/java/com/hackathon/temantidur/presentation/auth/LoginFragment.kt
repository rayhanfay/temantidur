package com.hackathon.temantidur.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.data.chat.ChatStorageManager // Import ChatStorageManager
import com.hackathon.temantidur.data.emotion.EmotionStorageManager // Import EmotionStorageManager
import com.hackathon.temantidur.databinding.FragmentLoginBinding
import com.hackathon.temantidur.domain.AuthResult
import com.hackathon.temantidur.presentation.ViewModelFactory
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import com.hackathon.temantidur.utils.PasswordToggleHelper
import com.hackathon.temantidur.utils.dialogs.*
import kotlinx.coroutines.launch // Import launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: AuthViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var chatStorageManager: ChatStorageManager
    private lateinit var emotionStorageManager: EmotionStorageManager
    private var loadingDialog: LoadingDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(this, ViewModelFactory())[AuthViewModel::class.java]
        sessionManager = SessionManager(requireContext())
        chatStorageManager = ChatStorageManager(requireContext())
        emotionStorageManager = EmotionStorageManager(requireContext())

        setupErrorClearingListeners()

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            if (validateInput(email, pass)) performLogin(email, pass)
        }

        binding.tvRegisterNow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.auth_container, RegisterFragment())
                .commit()
        }

        binding.loginCard.post {
            val cardWidth = binding.loginCard.width
            val newLogoWidth = cardWidth / 3
            binding.ivLogo.layoutParams = binding.ivLogo.layoutParams.apply {
                width = newLogoWidth
                height = newLogoWidth
            }
        }

        PasswordToggleHelper.setup(
            requireContext(),
            binding.tilPassword,
            binding.etPassword
        )
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            binding.tilEmail.isErrorEnabled = true
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            binding.tilEmail.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilEmail.error = null
            binding.tilEmail.isErrorEnabled = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            binding.tilPassword.isErrorEnabled = true
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_min_length)
            binding.tilPassword.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilPassword.error = null
            binding.tilPassword.isErrorEnabled = false
        }

        return isValid
    }

    private fun performLogin(email: String, password: String) {
        vm.login(email, password).observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> showLoading(true)
                is AuthResult.Success -> {
                    showLoading(false)
                    val user = result.user
                    sessionManager.createLoginSession(
                        user?.uid ?: "",
                        user?.username ?: "",
                        user?.email ?: ""
                    )

                    if (sessionManager.shouldClearAllUserData()) {
                        lifecycleScope.launch {
                            chatStorageManager.clearAllChatData()
                            emotionStorageManager.clearAllEmotionData()
                        }
                    }

                    SuccessDialogFragment.newInstance(
                        getString(R.string.login_success_title),
                        result.message
                    ) {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        activity?.finish()
                    }.show(parentFragmentManager, SuccessDialogFragment.TAG)
                }

                is AuthResult.Error -> {
                    showLoading(false)
                    handleLoginError(result.error)
                }

                else -> {}
            }
        }
    }

    private fun handleLoginError(errorCode: String) {
        clearAllErrors()

        when (errorCode) {
            "EMAIL_NOT_FOUND" -> {
                binding.tilEmail.error = getString(R.string.error_email_not_registered)
                binding.tilEmail.isErrorEnabled = true
                binding.etEmail.requestFocus()
            }

            "CREDENTIAL_INVALID" -> {
                binding.tilEmail.error = getString(R.string.error_invalid_credentials)
                binding.tilPassword.error = getString(R.string.error_invalid_credentials)
                binding.tilEmail.isErrorEnabled = true
                binding.tilPassword.isErrorEnabled = true
                binding.etEmail.requestFocus()
            }

            "EMAIL_FORMAT_INVALID", "FORMAT_EMAIL_INVALID" -> {
                binding.tilEmail.error = getString(R.string.error_email_invalid)
                binding.tilEmail.isErrorEnabled = true
                binding.etEmail.requestFocus()
            }

            "TOO_MANY_ATTEMPTS" -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_too_many_attempts_title),
                    getString(R.string.error_too_many_attempts_message)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }

            "ACCOUNT_DISABLED" -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_account_disabled_title),
                    getString(R.string.error_account_disabled_message)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }

            "NETWORK_ERROR" -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_network_title),
                    getString(R.string.error_network_message)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }

            "DATA_USER_NOT_FOUND" -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_data_not_found_title),
                    getString(R.string.error_data_not_found_message)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }

            "LOGIN_FAILED_UNKNOWN" -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_login_failed_title),
                    getString(R.string.error_login_failed_unknown_message)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }

            else -> {
                FailedDialogFragment.newInstance(
                    getString(R.string.error_login_failed_title),
                    getString(R.string.error_login_failed_message, errorCode)
                ).show(parentFragmentManager, FailedDialogFragment.TAG)
            }
        }
    }

    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false
        binding.tilPassword.error = null
        binding.tilPassword.isErrorEnabled = false
    }

    private fun setupErrorClearingListeners() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilEmail.isErrorEnabled) {
                    binding.tilEmail.error = null
                    binding.tilEmail.isErrorEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilPassword.isErrorEnabled) {
                    binding.tilPassword.error = null
                    binding.tilPassword.isErrorEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            if (loadingDialog == null) loadingDialog = LoadingDialogFragment.newInstance()
            loadingDialog?.show(parentFragmentManager, LoadingDialogFragment.TAG)
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        binding.btnSignIn.isEnabled = !show
        binding.tvRegisterNow.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}