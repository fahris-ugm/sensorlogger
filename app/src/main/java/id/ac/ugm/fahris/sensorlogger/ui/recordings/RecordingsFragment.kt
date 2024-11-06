package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentRecordingsBinding

class RecordingsFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val recordingsViewModel: RecordingsViewModel by viewModels()
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingsAdapter: RecordingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recordingsRecyclerView = binding.recordingsRecyclerView
        recordingsRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        recordingsAdapter = RecordingsAdapter()
        recordingsRecyclerView.adapter = recordingsAdapter

        recordingsViewModel.allRecordData.observe(viewLifecycleOwner,
            Observer {
                recordItems -> recordItems?.let {
                    recordingsAdapter.submitList(it)
                }
            }
        )

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}