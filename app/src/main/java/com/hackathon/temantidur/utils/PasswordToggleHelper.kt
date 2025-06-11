package com.hackathon.temantidur.utils

import android.content.Context
import android.text.InputType
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hackathon.temantidur.R

object PasswordToggleHelper {

    fun setup(
        context: Context,
        textInputLayout: TextInputLayout,
        editText: TextInputEditText
    ) {
        var isPasswordVisible = false

        val eyeOn = ContextCompat.getDrawable(context, R.drawable.ic_eye_on)
        val eyeOff = ContextCompat.getDrawable(context, R.drawable.ic_eye_off)

        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
        textInputLayout.endIconDrawable = eyeOff

        textInputLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                textInputLayout.endIconDrawable = eyeOn
            } else {
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textInputLayout.endIconDrawable = eyeOff
            }
            editText.setSelection(editText.text?.length ?: 0)
        }
    }
}
