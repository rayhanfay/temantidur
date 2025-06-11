package com.hackathon.temantidur.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import androidx.recyclerview.widget.RecyclerView

class WrapContentRecyclerView(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs) {
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val expandedHeightSpec = MeasureSpec.makeMeasureSpec(
            Integer.MAX_VALUE shr 2, MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, expandedHeightSpec)
    }
}
