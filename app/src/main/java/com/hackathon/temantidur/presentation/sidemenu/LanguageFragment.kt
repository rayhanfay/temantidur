package com.hackathon.temantidur.presentation.sidemenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.databinding.FragmentLanguageBinding

class LanguageFragment : DialogFragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomDialogTheme)
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentLanguage = sessionManager.getLanguage()
        updateButtonSelection(currentLanguage)
        updateLanguageButtons()

        binding.btnIndonesian.setOnClickListener {
            onLanguageSelected("id")
            updateLanguageButtons()
        }

        binding.btnEnglish.setOnClickListener {
            onLanguageSelected("en")
            updateLanguageButtons()
        }
    }

    private fun updateLanguageButtons() {
        val currentLanguage = sessionManager.getLanguage()

        if (currentLanguage == "id") {
            binding.btnIndonesian.text = "Bahasa Indonesia (Terpilih)"
            binding.btnEnglish.text = "Bahasa Inggris"
        } else {
            binding.btnIndonesian.text = "Indonesian"
            binding.btnEnglish.text = "English (Chosen)"
        }
    }

    private fun onLanguageSelected(language: String) {
        updateButtonSelection(language)
        sessionManager.setLanguage(language)
        dismiss()
        (activity as? OnLanguageChangeListener)?.onLanguageChanged()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            val marginPx = resources.getDimensionPixelSize(R.dimen.dialog_horizontal_margin)
            setMargins(marginPx, topMargin, marginPx, bottomMargin)
            binding.root.layoutParams = this
        }
    }

    private fun updateButtonSelection(selectedLanguage: String?) {
        val selectedColor = resources.getColorStateList(R.color.darkblue, null)
        val unselectedColor = resources.getColorStateList(R.color.softmagenta, null)
        val selectedTextColor = requireContext().getColor(R.color.white)
        val unselectedTextColor = requireContext().getColor(R.color.white)

        binding.btnIndonesian.apply {
            backgroundTintList = if (selectedLanguage == "id") selectedColor else unselectedColor
            setTextColor(if (selectedLanguage == "id") selectedTextColor else unselectedTextColor)
        }

        binding.btnEnglish.apply {
            backgroundTintList = if (selectedLanguage == "en") selectedColor else unselectedColor
            setTextColor(if (selectedLanguage == "en") selectedTextColor else unselectedTextColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnLanguageChangeListener {
        fun onLanguageChanged()
    }

    companion object {
        const val TAG = "LanguageFragment"
    }
}