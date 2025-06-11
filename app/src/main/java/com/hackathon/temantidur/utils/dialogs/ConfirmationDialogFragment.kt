package com.hackathon.temantidur.utils.dialogs

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.hackathon.temantidur.R

class ConfirmationDialogFragment : DialogFragment() {

    private var title: String? = null
    private var message: String? = null
    private var onYesClickListener: (() -> Unit)? = null
    private var onNoClickListener: (() -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    companion object {
        const val TAG = "ConfirmationDialogFragment"

        fun newInstance(
            title: String,
            message: String,
            onYes: (() -> Unit)? = null,
            onNo: (() -> Unit)? = null,
            onDismiss: (() -> Unit)? = null
        ): ConfirmationDialogFragment {
            return ConfirmationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("message", message)
                }
                this.onYesClickListener = onYes
                this.onNoClickListener = onNo
                this.onDismissListener = onDismiss
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString("title")
            message = it.getString("message")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val btnYes: Button = view.findViewById(R.id.btn_yes)
        val btnNo: Button = view.findViewById(R.id.btn_no)

        tvTitle.text = title
        tvMessage.text = message

        btnYes.setOnClickListener {
            onYesClickListener?.invoke()
            dismiss()
        }

        btnNo.setOnClickListener {
            onNoClickListener?.invoke()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }
}