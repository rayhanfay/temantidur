package com.hackathon.temantidur.presentation.sidemenu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.hackathon.temantidur.BuildConfig
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentAboutUsBinding

class AboutUsFragment : Fragment() {

    private var _binding: FragmentAboutUsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        binding.navProfile1.contentDescription =
            getString(R.string.github_profile_of_person_1, getString(R.string.people_1))
        binding.navProfile2.contentDescription =
            getString(R.string.github_profile_of_person_2, getString(R.string.people_2))
        binding.navProfile3.contentDescription =
            getString(R.string.github_profile_of_person_3, getString(R.string.people_3))

        binding.navProfile1.setOnClickListener {
            openLink("https://github.com/rayhanfay")
        }
        binding.navProfile2.setOnClickListener {
            openLink("https://github.com/hashfiayd")
        }
        binding.navProfile3.setOnClickListener {
            openLink("https://github.com/AgusSyuhada")
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}