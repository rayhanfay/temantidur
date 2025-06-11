package com.hackathon.temantidur.presentation.sidemenu

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.databinding.*
import com.hackathon.temantidur.domain.AuthResult
import com.hackathon.temantidur.presentation.ViewModelFactory
import com.hackathon.temantidur.presentation.auth.AuthViewModel
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import com.hackathon.temantidur.utils.PasswordToggleHelper
import com.hackathon.temantidur.utils.dialogs.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var authViewModel: AuthViewModel
    private var loadingDialog: LoadingDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        authViewModel = ViewModelProvider(this, ViewModelFactory()).get(AuthViewModel::class.java)

        loadUserData()

        binding.tvUserEmail.text = sessionManager.getUserEmail()
        binding.tvUserName.text = sessionManager.getUserName() ?: getString(R.string.user_default_name)

        binding.btnLogout.setOnClickListener {
            (activity as? MainActivity)?.showLogoutConfirmationDialog()
        }
        binding.btnChangeEmail.setOnClickListener { showChangeEmailDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnChangeUsername?.setOnClickListener { showChangeUsernameDialog() }

        binding.ivEditProfilePicture.setOnClickListener {
            showProfilePictureDialog()
        }
    }

    private fun setupPasswordToggle(
        context: Context,
        textInputLayout: TextInputLayout,
        editText: TextInputEditText
    ) {
        PasswordToggleHelper.setup(context, textInputLayout, editText)
    }

    private fun loadUserData() {
        binding.tvUserName.text = sessionManager.getUserName() ?: getString(R.string.user_default_name)
        binding.ivProfilePicture.setImageResource(sessionManager.getProfilePictureResId())

        authViewModel.getCurrentEmail().observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is AuthResult.Success -> {
                    binding.tvUserEmail.text = result.message
                    sessionManager.saveUserEmail(result.message)
                }
                is AuthResult.Error -> {
                    binding.tvUserEmail.text = sessionManager.getUserEmail() ?: getString(R.string.email_not_available)
                    Log.e("ProfileFragment", "Failed to load email from database: ${result.error}")
                }
                else -> {
                    // Loading state - optional: show loading indicator
                }
            }
        })
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) loadingDialog = LoadingDialogFragment.newInstance()
        if (!loadingDialog!!.isAdded) {
            loadingDialog?.show(childFragmentManager, LoadingDialogFragment.TAG)
        }
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun showSuccessDialog(title: String, message: String, onOk: (() -> Unit)? = null) {
        SuccessDialogFragment.newInstance(title, message, onOk)
            .show(childFragmentManager, SuccessDialogFragment.TAG)
    }

    private fun showFailedDialog(title: String, message: String, onOk: (() -> Unit)? = null) {
        FailedDialogFragment.newInstance(title, message, onOk)
            .show(childFragmentManager, FailedDialogFragment.TAG)
    }

    private fun showChangeUsernameDialog() {
        val dialogBinding = DialogChangeUsernameBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        val currentUsername = sessionManager.getUserName()
        dialogBinding.etCurrentUsername.setText(currentUsername)
        dialogBinding.etCurrentUsername.isEnabled = false

        dialogBinding.btnChangeUsername.setOnClickListener {
            val newUsername = dialogBinding.etNewUsername.text.toString().trim()
            when {
                newUsername.isEmpty() -> dialogBinding.etNewUsername.error =
                    getString(R.string.username_empty_error)

                newUsername.length < 3 -> dialogBinding.etNewUsername.error =
                    getString(R.string.username_min_length_error)

                newUsername.length > 20 -> dialogBinding.etNewUsername.error =
                    getString(R.string.username_max_length_error)

                !newUsername.matches(Regex("^[a-zA-Z0-9._]+$")) ->
                    dialogBinding.etNewUsername.error =
                        getString(R.string.username_invalid_chars_error)

                newUsername == currentUsername ->
                    dialogBinding.etNewUsername.error =
                        getString(R.string.username_same_as_current_error)

                else -> {
                    dialog.dismiss()
                    showLoadingDialog()
                    authViewModel.changeUsername(newUsername)
                        .observe(viewLifecycleOwner, Observer { result ->
                            dismissLoadingDialog()
                            when (result) {
                                is AuthResult.Success -> {
                                    sessionManager.saveUserName(newUsername)
                                    binding.tvUserName.text = newUsername
                                    showSuccessDialog(getString(R.string.success_title), result.message)
                                }

                                is AuthResult.Error ->
                                    showFailedDialog(getString(R.string.failed_title), result.error)

                                else -> {}
                            }
                        })
                }
            }
        }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showChangeEmailDialog() {
        val dialogBinding = DialogChangeEmailBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email
        dialogBinding.etCurrentEmail.setText(currentUserEmail)
        dialogBinding.etCurrentEmail.isEnabled = false

        setupPasswordToggle(requireContext(), dialogBinding.tilCurrentPassword, dialogBinding.etCurrentPassword)

        setupClearErrorOnTextChange(dialogBinding.etNewEmail, dialogBinding.tilNewEmail)
        setupClearErrorOnTextChange(dialogBinding.etCurrentPassword, dialogBinding.tilCurrentPassword)

        dialogBinding.btnChangeEmail.setOnClickListener {
            val newEmail = dialogBinding.etNewEmail.text.toString().trim()
            val currentPassword = dialogBinding.etCurrentPassword.text.toString().trim()

            dialogBinding.tilNewEmail.error = null
            dialogBinding.tilCurrentPassword.error = null

            var hasError = false

            if (newEmail.isEmpty()) {
                dialogBinding.tilNewEmail.error = getString(R.string.new_email_empty_error)
                dialogBinding.tilNewEmail.isErrorEnabled = true
                hasError = true
            } else if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                dialogBinding.tilNewEmail.error = getString(R.string.email_invalid_format_error)
                dialogBinding.tilNewEmail.isErrorEnabled = true
                hasError = true
            }

            if (currentPassword.isEmpty()) {
                dialogBinding.tilCurrentPassword.error = getString(R.string.password_required_verification_error)
                dialogBinding.tilCurrentPassword.isErrorEnabled = true
                hasError = true
            }

            if (!hasError) {
                dialog.dismiss()
                showLoadingDialog()
                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
                reauthenticateAndChangeEmail(currentUserEmail!!, currentPassword, newEmail)
            }
        }
        dialogBinding.btnCancel?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun reauthenticateAndChangeEmail(
        currentEmail: String,
        password: String,
        newEmail: String
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            dismissLoadingDialog()
            showFailedDialog("Error", getString(R.string.user_not_found_error))
            return
        }
        val credential = EmailAuthProvider.getCredential(currentEmail, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    changeEmailWithVerification(newEmail)
                } else {
                    dismissLoadingDialog()
                    val errorMessage = when (reauthTask.exception?.message) {
                        "The password is invalid or the user does not have a password." -> getString(R.string.wrong_password_error)
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> getString(R.string.network_error)
                        else -> getString(R.string.password_verification_failed_error, reauthTask.exception?.message)
                    }
                    showFailedDialog(getString(R.string.failed_title), errorMessage)
                    Log.e("ProfileFragment", "Re-authentication failed", reauthTask.exception)
                }
            }
    }

    private fun changeEmailWithVerification(newEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            dismissLoadingDialog()
            showFailedDialog(getString(R.string.failed_title), getString(R.string.user_not_found_error))
            return
        }
        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                dismissLoadingDialog()
                if (task.isSuccessful) {
                    showSuccessDialog(
                        getString(R.string.email_verification_sent_title),
                        getString(R.string.email_verification_sent_message, newEmail)
                    ) { showEmailChangeInstructions() }
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." -> getString(R.string.email_already_in_use_error)
                        "The email address is badly formatted." -> getString(R.string.error_email_invalid)
                        else -> task.exception?.message ?: getString(R.string.send_verification_failed_error)
                    }
                    showFailedDialog(getString(R.string.failed_title), errorMessage)
                    Log.e("ProfileFragment", "Failed to send email verification", task.exception)
                }
            }
    }

    private fun showEmailChangeInstructions() {
        val dialogBinding = DialogEmailChangeInstructionsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.setCancelable(true)
        dialogBinding.btnOk.text = getString(R.string.logout)
        dialogBinding.btnOk.setOnClickListener {
            dialog.dismiss()
            (activity as? MainActivity)?.showLogoutConfirmationDialog()
        }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showProfilePictureDialog() {
        val dialogBinding = DialogChooseProfilePictureBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        val currentProfileResId = sessionManager.getProfilePictureResId()
        dialogBinding.ivPreview.setImageResource(currentProfileResId)

        val avatarMap = mapOf(
            dialogBinding.containerGirl1 to Pair(R.drawable.ic_girl_1, dialogBinding.ivGirl1),
            dialogBinding.containerGirl2 to Pair(R.drawable.ic_girl_2, dialogBinding.ivGirl2),
            dialogBinding.containerMan1 to Pair(R.drawable.ic_man_1, dialogBinding.ivMan1),
        )

        var selectedDrawableResId: Int = currentProfileResId

        avatarMap.values.find { it.first == currentProfileResId }?.let { (_, imageView) ->
            (imageView.parent as? ViewGroup)?.findViewById<View>(R.id.view_selected_border)?.visibility = View.VISIBLE
        }

        avatarMap.forEach { (container, pair) ->
            val (drawableResId, imageView) = pair
            container.setOnClickListener {
                selectedDrawableResId = drawableResId
                dialogBinding.ivPreview.setImageResource(drawableResId)
                dialogBinding.viewSelectedBorder.visibility = View.VISIBLE

                avatarMap.values.forEach { (_, iv) ->
                    (iv.parent as? ViewGroup)?.findViewById<View>(R.id.view_selected_border)?.visibility = View.INVISIBLE
                }

                (imageView.parent as? ViewGroup)?.findViewById<View>(R.id.view_selected_border)?.visibility = View.VISIBLE
            }
        }

        dialogBinding.btnSave.setOnClickListener {
            if (selectedDrawableResId != currentProfileResId) {
                binding.ivProfilePicture.setImageResource(selectedDrawableResId)
                sessionManager.saveProfilePictureResId(selectedDrawableResId)

                showSuccessDialog(
                    getString(R.string.success_title),
                    getString(R.string.profile_picture_updated)
                )
            }
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        setupPasswordToggle(requireContext(), dialogBinding.tilCurrentPassword, dialogBinding.etCurrentPassword)
        setupPasswordToggle(requireContext(), dialogBinding.tilNewPassword, dialogBinding.etNewPassword)
        setupPasswordToggle(requireContext(), dialogBinding.tilConfirmNewPassword, dialogBinding.etConfirmNewPassword)

        setupClearErrorOnTextChange(dialogBinding.etCurrentPassword, dialogBinding.tilCurrentPassword)
        setupClearErrorOnTextChange(dialogBinding.etNewPassword, dialogBinding.tilNewPassword)
        setupClearErrorOnTextChange(dialogBinding.etConfirmNewPassword, dialogBinding.tilConfirmNewPassword)

        dialogBinding.btnChangePassword.setOnClickListener {
            val currentPassword = dialogBinding.etCurrentPassword.text.toString().trim()
            val newPassword = dialogBinding.etNewPassword.text.toString().trim()
            val confirmNewPassword = dialogBinding.etConfirmNewPassword.text.toString().trim()

            dialogBinding.tilCurrentPassword.error = null
            dialogBinding.tilNewPassword.error = null
            dialogBinding.tilConfirmNewPassword.error = null

            var hasError = false

            if (currentPassword.isEmpty()) {
                dialogBinding.tilCurrentPassword.error = getString(R.string.current_password_empty_error)
                dialogBinding.tilCurrentPassword.isErrorEnabled = true
                hasError = true
            }

            if (newPassword.isEmpty()) {
                dialogBinding.tilNewPassword.error = getString(R.string.new_password_empty_error)
                dialogBinding.tilNewPassword.isErrorEnabled = true
                hasError = true
            } else if (newPassword.length < 6) {
                dialogBinding.tilNewPassword.error = getString(R.string.password_min_length_error)
                dialogBinding.tilNewPassword.isErrorEnabled = true
                hasError = true
            }

            if (confirmNewPassword.isEmpty()) {
                dialogBinding.tilConfirmNewPassword.error = getString(R.string.confirm_password_empty_error)
                dialogBinding.tilConfirmNewPassword.isErrorEnabled = true
                hasError = true
            } else if (newPassword != confirmNewPassword) {
                dialogBinding.tilConfirmNewPassword.error = getString(R.string.password_mismatch_error)
                dialogBinding.tilConfirmNewPassword.isErrorEnabled = true
                hasError = true
            }

            if (!hasError) {
                dialog.dismiss()
                showLoadingDialog()
                authViewModel.changePassword(currentPassword, newPassword)
                    .observe(viewLifecycleOwner, Observer { result ->
                        dismissLoadingDialog()
                        when (result) {
                            is AuthResult.Success ->
                                showSuccessDialog(getString(R.string.success_title), result.message)
                            is AuthResult.Error ->
                                showFailedDialog(getString(R.string.failed_title), result.error)
                            else -> {}
                        }
                    })
            }
        }

        dialogBinding.btnCancel?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupClearErrorOnTextChange(
        editText: TextInputEditText,
        textInputLayout: TextInputLayout
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textInputLayout.isErrorEnabled) {
                    textInputLayout.error = null
                    textInputLayout.isErrorEnabled = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dismissLoadingDialog()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}