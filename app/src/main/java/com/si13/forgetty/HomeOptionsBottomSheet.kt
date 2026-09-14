package com.si13.forgetty

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

/** Single entry point for the Home controls shown in the Figma redesign. */
class HomeOptionsBottomSheet : BottomSheetDialogFragment() {
    private var showCompleted = false
    private var sortMode = TaskSortMode.DUE_DATE
    private var selectedList: String? = null
    private val selectedTags = linkedSetOf<String>()
    private var availableLists = arrayListOf<String>()
    private var availableTags = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCompleted = arguments?.getBoolean(ARG_SHOW_COMPLETED) ?: false
        sortMode = TaskSortMode.fromKey(arguments?.getString(ARG_SORT_MODE).orEmpty())
        selectedList = arguments?.getString(ARG_SELECTED_LIST)
        selectedTags += arguments?.getStringArrayList(ARG_SELECTED_TAGS).orEmpty()
        availableLists = arguments?.getStringArrayList(ARG_AVAILABLE_LISTS) ?: arrayListOf()
        availableTags = arguments?.getStringArrayList(ARG_AVAILABLE_TAGS) ?: arrayListOf()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val root = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_home_options, null)
        dialog.setContentView(root)
        configureWindow(dialog.window)
        bind(root)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply {
                    isFitToContents = true
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        return dialog
    }

    private fun configureWindow(window: Window?) {
        window ?: return
        window.navigationBarColor = requireContext().getColor(R.color.forgetty_surface)
        window.setDimAmount(0.36f)
    }

    private fun bind(root: View) {
        ViewCompat.setAccessibilityPaneTitle(root, getString(R.string.home_options))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
        root.findViewById<View>(R.id.home_options_close).setOnClickListener { dismiss() }
        root.findViewById<TextView>(R.id.home_options_sort_value).setText(sortMode.labelRes())
        root.findViewById<TextView>(R.id.home_options_list_value).text =
            selectedList ?: getString(R.string.all_tasks)
        root.findViewById<TextView>(R.id.home_options_tags_value).text =
            if (selectedTags.isEmpty()) getString(R.string.any_tag) else selectedTags.joinToString()

        root.findViewById<View>(R.id.home_options_sort_row).setOnClickListener {
            SortMenuBottomSheet.show(parentFragmentManager, sortMode)
            dismiss()
        }
        val openFilters = View.OnClickListener {
            FilterBottomSheet.show(
                parentFragmentManager,
                selectedList,
                selectedTags,
                availableLists,
                availableTags
            )
            dismiss()
        }
        root.findViewById<View>(R.id.home_options_lists_row).setOnClickListener(openFilters)
        root.findViewById<View>(R.id.home_options_tags_row).setOnClickListener(openFilters)

        val completedSwitch = root.findViewById<SwitchMaterial>(R.id.home_options_show_completed)
        completedSwitch.isChecked = showCompleted
        completedSwitch.setOnCheckedChangeListener { _, checked -> showCompleted = checked }
        root.findViewById<View>(R.id.home_options_completed_row).setOnClickListener {
            completedSwitch.isChecked = !completedSwitch.isChecked
        }
        root.findViewById<MaterialButton>(R.id.home_options_reset).setOnClickListener {
            showCompleted = false
            sendResult()
            dismiss()
        }
        root.findViewById<MaterialButton>(R.id.home_options_done).setOnClickListener {
            sendResult()
            dismiss()
        }
    }

    private fun sendResult() {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply { putBoolean(RESULT_SHOW_COMPLETED, showCompleted) }
        )
    }

    private fun TaskSortMode.labelRes(): Int = when (this) {
        TaskSortMode.DUE_DATE -> R.string.sort_due_date
        TaskSortMode.PRIORITY_FIRST -> R.string.sort_priority_first
        TaskSortMode.NEWEST_FIRST -> R.string.sort_newest_first
        TaskSortMode.OLDEST_FIRST -> R.string.sort_oldest_first
        TaskSortMode.ALPHABETICAL -> R.string.sort_alphabetical
    }

    companion object {
        const val TAG = "HomeOptionsBottomSheet"
        const val RESULT_KEY = "home_options_result"
        const val RESULT_SHOW_COMPLETED = "show_completed"
        private const val ARG_SHOW_COMPLETED = "arg_show_completed"
        private const val ARG_SORT_MODE = "arg_sort_mode"
        private const val ARG_SELECTED_LIST = "arg_selected_list"
        private const val ARG_SELECTED_TAGS = "arg_selected_tags"
        private const val ARG_AVAILABLE_LISTS = "arg_available_lists"
        private const val ARG_AVAILABLE_TAGS = "arg_available_tags"

        fun show(
            fragmentManager: FragmentManager,
            showCompleted: Boolean,
            sortMode: TaskSortMode,
            selectedList: String?,
            selectedTags: Set<String>,
            availableLists: List<String>,
            availableTags: List<String>
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            HomeOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_COMPLETED, showCompleted)
                    putString(ARG_SORT_MODE, sortMode.key)
                    putString(ARG_SELECTED_LIST, selectedList)
                    putStringArrayList(ARG_SELECTED_TAGS, ArrayList(selectedTags))
                    putStringArrayList(ARG_AVAILABLE_LISTS, ArrayList(availableLists))
                    putStringArrayList(ARG_AVAILABLE_TAGS, ArrayList(availableTags))
                }
            }.show(fragmentManager, TAG)
        }
    }
}
