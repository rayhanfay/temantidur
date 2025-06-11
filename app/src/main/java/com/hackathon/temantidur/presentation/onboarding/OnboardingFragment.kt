package com.hackathon.temantidur.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.hackathon.temantidur.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private var title: String? = null
    private var titleDesc: String? = null
    private var description: String? = null
    @DrawableRes
    private var imageRes: Int = 0
    private var isLastSlide: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            titleDesc = it.getString(ARG_TITLE_DESC)
            description = it.getString(ARG_DESCRIPTION)
            imageRes = it.getInt(ARG_IMAGE_RES)
            isLastSlide = it.getBoolean(ARG_IS_LAST_SLIDE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = title
        binding.tvTitleDesc.text = titleDesc
        binding.tvDescription.text = description
        binding.ivOnboarding.setImageResource(imageRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TITLE_DESC = "title_desc"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_IMAGE_RES = "image_res"
        private const val ARG_IS_LAST_SLIDE = "is_last_slide"

        @JvmStatic
        fun newInstance(
            title: String,
            titleDesc: String,
            description: String,
            @DrawableRes imageRes: Int,
            isLastSlide: Boolean = false
        ) = OnboardingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_TITLE_DESC, titleDesc)
                putString(ARG_DESCRIPTION, description)
                putInt(ARG_IMAGE_RES, imageRes)
                putBoolean(ARG_IS_LAST_SLIDE, isLastSlide)
            }
        }
    }
}