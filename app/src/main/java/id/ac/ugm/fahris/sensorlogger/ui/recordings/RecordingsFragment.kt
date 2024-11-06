package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.RecordData
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentRecordingsBinding
import kotlinx.coroutines.launch

class RecordingsFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val recordingsViewModel: RecordingsViewModel by viewModels()
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingsAdapter: RecordingsAdapter
    private var actionMode: ActionMode? = null

    // SQLite Room database
    private lateinit var appDatabase: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recordingsRecyclerView = binding.recordingsRecyclerView
        recordingsRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        recordingsAdapter = RecordingsAdapter(
            onItemClickListener = { recordData ->openRecordDetailActivity(recordData.recordId)},
            onToggleSelection = { position -> toggleSelection(position) }
            )
        recordingsRecyclerView.adapter = recordingsAdapter

        recordingsViewModel.allRecordData.observe(viewLifecycleOwner
        ) { recordItems ->
            recordItems?.let {
                recordingsAdapter.submitList(it)
            }
        }

        appDatabase = AppDatabase.getDatabase(requireContext())

        return root
    }

    private fun startActionMode() {
        actionMode = activity?.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.multi_select_record_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_delete -> {
                        deleteSelectedItems()
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                recordingsAdapter.clearSelection()
                actionMode = null
            }
        })
    }

    private fun deleteSelectedItems() {
        val selectedItems = recordingsAdapter.getSelectedItems()
        //TODO
        val selectedCount = selectedItems.size
        if (selectedCount == 0) return

        // Show confirmation dialog
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Confirmation")
            setMessage("Are you sure you want to delete $selectedCount selected item(s)?")

            setPositiveButton("Delete") { _, _ ->
                // Proceed with deletion if confirmed
                performDeletion(selectedItems)
            }

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()  // Close the dialog if canceled
            }

            create()
            show()
        }

    }
    private fun performDeletion(selectedItems: List<RecordData>) {
        lifecycleScope.launch {
            appDatabase.recordDataDao().deleteRecordDataByIds(selectedItems.map { it.recordId })
            Toast.makeText(context, "${selectedItems.size} items", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSelection(position: Int) {
        //recordingsAdapter.toggleSelection(position)
        Log.d("RecordingsAdapter", "toggleSelection called with position: $position")
        if (recordingsAdapter.selectedItemsIndex.contains(position)) {
            recordingsAdapter.selectedItemsIndex.remove(position)
        } else {
            recordingsAdapter.selectedItemsIndex.add(position)
        }
        recordingsAdapter.notifyItemChanged(position)
        val selectedCount = recordingsAdapter.getSelectedItems().size

        if (selectedCount > 0 && actionMode == null) {
            startActionMode()
        }

        actionMode?.title = "$selectedCount selected"

        if (selectedCount == 0) {
            actionMode?.finish()
        }
    }

    private fun openRecordDetailActivity(recordingId: Long) {
        //
        Log.d("RecordingsFragment", "Recording ID: $recordingId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}