package com.example.androidtodo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.TextView
import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageView
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val addSectionButton = findViewById<Button>(R.id.buttonAddNewSection)
        addSectionButton.setOnClickListener {
            showAddSectionDialog()
        }
    }

    private fun showAddSectionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Section")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, which ->
            val sectionName = input.text.toString()
            addSection(sectionName)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addSection(sectionName: String) {
        val layout = findViewById<LinearLayout>(R.id.sectionButtonLayout)

        // Create and add the separator view if not the first button
        if (layout.childCount > 0) {
            val separator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.section_margin_top)
                )
            }
            layout.addView(separator)
        }

        // Create button using style
        val button = Button(this, null, 0, R.style.SectionButtonStyle)
        button.text = sectionName

        button.setOnClickListener {
            val intent = Intent(this, SectionActivity::class.java)
            intent.putExtra("SECTION_NAME", sectionName)  // Pass the section name to the new Activity
            startActivity(intent)
        }

        layout.addView(button)
    }
}

class SectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section)

        // Get the section name from the Intent
        val sectionName = intent.getStringExtra("SECTION_NAME")

        // Set the section name to the TextView
        val sectionTitleView = findViewById<TextView>(R.id.sectionTitleText)
        sectionTitleView.text = sectionName

        val addTaskButton = findViewById<Button>(R.id.buttonAddNewTask)
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        val editSectionButton = findViewById<ImageButton>(R.id.settingsButton)
        editSectionButton.setOnClickListener{
            editSectionName(sectionTitleView ,sectionName)
        }

        val backToSectionButton = findViewById<ImageButton>(R.id.backButton)
        backToSectionButton.setOnClickListener{
            finish()
        }

    }

    private fun editSectionName(sectionView: View, originalSectionName: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Section Name")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(originalSectionName)
        builder.setView(input)

        builder.setPositiveButton("Apply") { dialog, which ->
            // Update the section name with new value
            val newSectionName = input.text.toString()
            val textViewSectionName = sectionView.findViewById<TextView>(R.id.sectionTitleText)
            textViewSectionName.text = newSectionName
        }

        builder.setNegativeButton("Delete") { dialog, which ->
            // Handle the delete action: remove the section view or mark it as deleted
            dialog.cancel()
            showDeleteConfirmationDialog(sectionView, originalSectionName)
        }

        builder.show()
    }

    private fun showDeleteConfirmationDialog(sectionView: View, sectionName: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Are you sure you want to delete the section '$sectionName'? This action cannot be undone.")

        builder.setPositiveButton("Delete") { dialog, which ->
            // Code to delete the section
            dialog.cancel()
            finish()
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Task")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_task_dialog, null)
        val taskInput = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)
        val dateInput = dialogView.findViewById<EditText>(R.id.editTextDueDate)

        builder.setView(dialogView)

        builder.setPositiveButton("Add", null)  // Set to null, we'll override it later
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()

        // Get the Add button and initially disable it
        val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        addButton.isEnabled = false

        // TextWatcher to validate date input
        dateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val dateText = dateInput.text.toString()
                if (isValidDate(dateText)) {
                    dateInput.error = null
                    addButton.isEnabled = true
                } else {
                    dateInput.error = "Invalid date format"
                    addButton.isEnabled = false
                }
            }
        })

        // Set the click listener for the Add button
        addButton.setOnClickListener {
            val taskText = taskInput.text.toString()
            val dueDate = dateInput.text.toString()
            if (isValidDate(dueDate)) {
                addTask(taskText, dueDate)  // Make sure this function exists and is correctly implemented
                dialog.dismiss()
            } else {
                dateInput.error = "Invalid date format"
            }
        }
    }

    // Function to validate date format
    private fun isValidDate(date: String): Boolean {
        val dateFormats = listOf(
            SimpleDateFormat("DDmmyyyy", Locale.ENGLISH),
            SimpleDateFormat("DD/mm/yyyy", Locale.ENGLISH)
        )
        return dateFormats.any { format ->
            try {
                format.isLenient = false
                format.parse(date)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun addTask(taskDescription: String, taskDueDate: String) {
        Log.i("Task", "Description: $taskDescription")
        Log.i("Task", "Due date: $taskDueDate")

        val layout = findViewById<LinearLayout>(R.id.taskLayout)

        if (layout.childCount > 0) {
            val separator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.section_margin_top)
                )
            }
            layout.addView(separator)
        }

        val taskView = LayoutInflater.from(this).inflate(R.layout.task_item, layout, false)

        val textViewDescription = taskView.findViewById<TextView>(R.id.textViewTaskDescription)
        textViewDescription.text = taskDescription
        val textViewDueDate = taskView.findViewById<TextView>(R.id.textViewDueDate)
        textViewDueDate.text = taskDueDate

        val checkBox = taskView.findViewById<CheckBox>(R.id.checkBoxTask)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Apply strikethrough and adjust alpha based on checked state
            textViewDescription.paintFlags = if (isChecked)
                textViewDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                textViewDescription.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            // Change the opacity of the entire task item
            taskView.alpha = if (isChecked) 0.5f else 1.0f
        }


        // Add edit functionality
        val editImage = taskView.findViewById<ImageView>(R.id.imageEdit)
        editImage.setOnClickListener {
            // Open dialog to edit task
            editTask(taskView, taskDescription, taskDueDate)
        }



        // Add delete functionality
        val deleteImage = taskView.findViewById<ImageView>(R.id.imageDelete)
        deleteImage.setOnClickListener {
            // Remove the task view and possibly the separator
            layout.removeView(taskView)
            // Optional: Remove the separator view if needed
        }

        // Add the filled task view to the layout
        layout.addView(taskView)

    }

    private fun editTask(taskView: View, originalDescription: String, originalDueDate: String) {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.add_task_dialog, null)
        val inputDescription = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)
        val inputDueDate = dialogView.findViewById<EditText>(R.id.editTextDueDate)

        // Set the current values
        inputDescription.setText(originalDescription)
        inputDueDate.setText(originalDueDate)

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Edit Task")
        dialog.setView(dialogView)
        dialog.setPositiveButton("Save") { _, _ ->
            // Update the task with new values
            val newDescription = inputDescription.text.toString()
            val newDueDate = inputDueDate.text.toString()

            // Find and update the existing views
            val textViewDescription = taskView.findViewById<TextView>(R.id.textViewTaskDescription)
            val textViewDueDate = taskView.findViewById<TextView>(R.id.textViewDueDate)
            textViewDescription.text = newDescription
            textViewDueDate.text = newDueDate

            // Optionally reset checkbox and alpha
            val checkBox = taskView.findViewById<CheckBox>(R.id.checkBoxTask)
            checkBox.isChecked = false
            taskView.alpha = 1.0f
        }
        dialog.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }

}
