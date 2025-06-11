package com.hackathon.temantidur.utils.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.hackathon.temantidur.R

class LoadingDialogFragment : DialogFragment() {

    private var onDismissListener: (() -> Unit)? = null

    companion object {
        const val TAG = "LoadingDialogFragment"

        fun newInstance(onDismiss: (() -> Unit)? = null): LoadingDialogFragment {
            return LoadingDialogFragment().apply {
                this.onDismissListener = onDismiss
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_loading, container, false)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }
}