package com.hackathon.temantidur.presentation.auth

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentRegisterBinding
import com.hackathon.temantidur.domain.AuthResult
import com.hackathon.temantidur.presentation.ViewModelFactory
import com.hackathon.temantidur.utils.PasswordToggleHelper
import com.hackathon.temantidur.utils.dialogs.*

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: AuthViewModel
    private var loadingDialog: LoadingDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(this, ViewModelFactory())[AuthViewModel::class.java]

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            if (validateInput(username, email, password, confirmPassword)) {
                performRegister(username, email, password)
            }
        }

        binding.tvLoginNow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }

        binding.registerCard.post {
            val cardWidth = binding.registerCard.width
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

        PasswordToggleHelper.setup(
            requireContext(),
            binding.tilConfirmPassword,
            binding.etConfirmPassword
        )
    }

    private fun validateInput(
        username: String, email: String, password: String, confirmPassword: String
    ): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_username_required)
            binding.tilUsername.isErrorEnabled = true
            isValid = false
        } else if (username.length < 3) {
            binding.tilUsername.error = getString(R.string.error_username_min_length)
            binding.tilUsername.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilUsername.error = null
            binding.tilUsername.isErrorEnabled = false
        }

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

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_confirm_password_required)
            binding.tilConfirmPassword.isErrorEnabled = true
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            binding.tilConfirmPassword.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
            binding.tilConfirmPassword.isErrorEnabled = false
        }

        return isValid
    }

    private fun performRegister(username: String, email: String, password: String) {
        vm.register(username, email, password).observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> showLoading(true)
                is AuthResult.OtpRequired -> {
                    showLoading(false)
                    SuccessDialogFragment.newInstance(getString(R.string.dialog_registration_success), result.message) {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.auth_container, LoginFragment())
                            .commit()
                    }.show(parentFragmentManager, SuccessDialogFragment.TAG)
                }

                is AuthResult.Error -> {
                    showLoading(false)
                    FailedDialogFragment.newInstance(getString(R.string.dialog_registration_failed), result.error)
                        .show(parentFragmentManager, FailedDialogFragment.TAG)
                }

                else -> showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            if (loadingDialog == null) loadingDialog = LoadingDialogFragment.newInstance()
            loadingDialog?.show(parentFragmentManager, LoadingDialogFragment.TAG)
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        binding.btnRegister.isEnabled = !show
        binding.tvLoginNow.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}