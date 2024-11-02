package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentLoggerBinding

class LoggerFragment : Fragment() {

    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val loggerViewModel =
            ViewModelProvider(this).get(LoggerViewModel::class.java)

        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textLogger
        loggerViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}