package com.si13.forgetty

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class TagManagerBottomSheet : BottomSheetDialogFragment() {
    private lateinit var store: TaskTagStore
    private lateinit var repository: TaskRepository
    private lateinit var content: LinearLayout
    private var tasks: List<Task> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TaskTagStore.create(requireContext())
        repository = TaskRepository.create(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val root = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_tag_manager, null)
        dialog.setContentView(root)
        dialog.window?.apply {
            navigationBarColor = requireContext().getColor(R.color.forgetty_surface)
            setDimAmount(0.36f)
        }
        root.findViewById<View>(R.id.tag_manager_close).setOnClickListener { dismiss() }
        root.findViewById<MaterialButton>(R.id.tag_manager_create).setOnClickListener { showEditor(null) }
        content = root.findViewById(R.id.tag_manager_content)
        ViewCompat.setAccessibilityPaneTitle(root, getString(R.string.manage_tags))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
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
        lifecycleScope.launch {
            tasks = repository.getTasks()
            store.ensureNames(tasks.flatMap(Task::tags))
            render()
        }
        return dialog
    }

    private fun render() {
        if (!::content.isInitialized) return
        content.removeAllViews()
        store.getTags().forEach { tag -> content.addView(tagCard(tag), matchWidth().apply { bottomMargin = dp(8) }) }
        content.addView(TextView(requireContext()).apply {
            text = getString(R.string.delete_tag_message)
            textSize = 12f
            setTextColor(context.getColor(R.color.forgetty_text_secondary))
            setBackgroundColor(context.getColor(R.color.forgetty_primary_container))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }, matchWidth().apply { topMargin = dp(4) })
    }

    private fun tagCard(tag: TaskTagDefinition): View = MaterialCardView(requireContext()).apply {
        radius = dp(14).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = context.getColor(R.color.forgetty_outline_variant)
        setCardBackgroundColor(context.getColor(R.color.forgetty_surface))
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(14), 0, dp(4), 0)
            addView(View(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(tag.color))
                }
            }, LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(12) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = tag.name
                    textSize = 15f
                    setTextColor(context.getColor(R.color.forgetty_text_primary))
                })
                addView(TextView(context).apply {
                    text = resources.getQuantityString(R.plurals.tasks_count, tasks.count { tag.name in it.tags }, tasks.count { tag.name in it.tags })
                    textSize = 12f
                    setTextColor(context.getColor(R.color.forgetty_text_secondary))
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageButton(context).apply {
                setImageResource(R.drawable.ic_more_vert)
                background = context.getDrawable(android.R.drawable.list_selector_background)
                contentDescription = getString(R.string.task_overflow_menu)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setOnClickListener { anchor -> showMenu(anchor, tag) }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        })
    }

    private fun showMenu(anchor: View, tag: TaskTagDefinition) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(R.string.edit).setOnMenuItemClickListener { showEditor(tag); true }
            menu.add(R.string.delete).setOnMenuItemClickListener { confirmDelete(tag); true }
        }.show()
    }

    private fun showEditor(tag: TaskTagDefinition?) {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        val field = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.tag_name)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val input = TextInputEditText(requireContext()).apply {
            setText(tag?.name.orEmpty())
            setSelection(text?.length ?: 0)
            hint = getString(R.string.tag_name_example)
            filters = arrayOf(InputFilter.LengthFilter(32))
            isSingleLine = true
        }
        field.addView(input)
        box.addView(field, matchWidth())
        val colors = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        var selectedColor = tag?.color ?: TaskTagStore.COLORS[store.getTags().size % TaskTagStore.COLORS.size]
        TaskTagStore.COLORS.forEach { color ->
            colors.addView(RadioButton(requireContext()).apply {
                id = View.generateViewId()
                buttonTintList = ColorStateList.valueOf(Color.parseColor(color))
                isChecked = color == selectedColor
                contentDescription = getString(R.string.select_list_color, color)
                setOnCheckedChangeListener { _, checked -> if (checked) selectedColor = color }
            }, RadioGroup.LayoutParams(dp(48), dp(48)))
        }
        box.addView(colors, matchWidth().apply { topMargin = dp(10) })
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (tag == null) R.string.create_new_tag else R.string.edit_tag)
            .setView(box)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(if (tag == null) R.string.add else R.string.save) { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isNotEmpty()) saveTag(tag, name, selectedColor)
            }.create()
        dialog.setOnShowListener {
            input.requestFocus()
            input.post { requireContext().getSystemService(InputMethodManager::class.java)?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }
        dialog.show()
    }

    private fun saveTag(tag: TaskTagDefinition?, name: String, color: String) {
        lifecycleScope.launch {
            if (tag == null) {
                store.create(name, color)
            } else {
                val change = store.update(tag.id, name, color) ?: return@launch
                tasks.filter { change.oldName in it.tags }.forEach { task ->
                    repository.updateTask(task.copy(tags = task.tags.map { if (it == change.oldName) change.updated.name else it }))
                }
                tasks = repository.getTasks()
            }
            render()
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle.EMPTY)
        }
    }

    private fun confirmDelete(tag: TaskTagDefinition) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_tag_title, tag.name))
            .setMessage(R.string.delete_tag_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    store.delete(tag.id)
                    tasks.filter { tag.name in it.tags }.forEach { task ->
                        repository.updateTask(task.copy(tags = task.tags.filterNot { it == tag.name }))
                    }
                    tasks = repository.getTasks()
                    render()
                    parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle.EMPTY)
                }
            }.show()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "TagManagerBottomSheet"
        const val RESULT_KEY = "tag_manager_result"
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) TagManagerBottomSheet().show(fragmentManager, TAG)
        }
    }
}
