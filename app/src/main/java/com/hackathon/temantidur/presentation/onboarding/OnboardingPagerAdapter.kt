package com.hackathon.temantidur.presentation.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hackathon.temantidur.R

class OnboardingPagerAdapter(
    private val fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val onboardingPages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            titleDesc = R.string.onboarding_title_desc_1,
            descriptionRes = R.string.onboarding_description_1,
            imageRes = R.drawable.cozy
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            titleDesc = R.string.onboarding_title_desc_2,
            descriptionRes = R.string.onboarding_description_2,
            imageRes = R.drawable.sharing
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            titleDesc = R.string.onboarding_title_desc_3,
            descriptionRes = R.string.onboarding_description_3,
            imageRes = R.drawable.calm,
            isLastSlide = true
        )
    )

    override fun getItemCount(): Int = onboardingPages.size

    override fun createFragment(position: Int): Fragment {
        val page = onboardingPages[position]
        return OnboardingFragment.newInstance(
            title = fragmentActivity.getString(page.titleRes),
            titleDesc = fragmentActivity.getString(page.titleDesc),
            description = fragmentActivity.getString(page.descriptionRes),
            imageRes = page.imageRes,
            isLastSlide = page.isLastSlide
        )
    }

    data class OnboardingPage(
        val titleRes: Int,
        val titleDesc: Int,
        val descriptionRes: Int,
        val imageRes: Int,
        val isLastSlide: Boolean = false
    )
}