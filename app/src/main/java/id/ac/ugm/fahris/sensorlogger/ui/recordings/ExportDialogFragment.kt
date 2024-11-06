import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import id.ac.ugm.fahris.sensorlogger.R

class ExportOptionsDialogFragment : DialogFragment() {

    private lateinit var editTextPrefix: EditText
    private lateinit var checkBoxShare: CheckBox
    private lateinit var buttonConfirm: Button
    private lateinit var buttonCancel: Button

    var onConfirmListener: ((String, Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_export, container, false)

        editTextPrefix = view.findViewById(R.id.editTextPrefix)
        checkBoxShare = view.findViewById(R.id.checkBoxShare)
        checkBoxShare.isChecked = true
        buttonConfirm = view.findViewById(R.id.buttonConfirm)
        buttonCancel = view.findViewById(R.id.buttonCancel)

        buttonConfirm.setOnClickListener {
            val prefix = editTextPrefix.text.toString()
            val shareFile = checkBoxShare.isChecked
            onConfirmListener?.invoke(prefix, shareFile)
            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        return view
    }
    override fun onStart() {
        super.onStart()
        // Set the dialog to be as wide as the screen
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(): ExportOptionsDialogFragment {
            return ExportOptionsDialogFragment()
        }
    }
}
