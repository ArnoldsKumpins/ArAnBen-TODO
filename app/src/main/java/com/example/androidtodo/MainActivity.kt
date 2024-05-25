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
import com.example.androidtodo.datatypes.Section
import com.example.androidtodo.database.SectionDAO
import androidx.lifecycle.lifecycleScope
import com.example.androidtodo.database.SectionDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var sectionDao: SectionDAO
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = SectionDatabase(applicationContext)
        sectionDao = db.getSectionDao()
        loadSections()

        val addSectionButton = findViewById<Button>(R.id.buttonAddNewSection)
        addSectionButton.setOnClickListener {
            showAddSectionDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data here
        loadSections()  // Assuming this method re-fetches the data from the database or server
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
        lifecycleScope.launch {
            sectionDao.createSection(Section(sectionTitle = sectionName)) // Save to DB
        }
    }
    private fun loadSections() {
        val layout = findViewById<LinearLayout>(R.id.sectionButtonLayout)
        layout.removeAllViews() // Clear existing buttons

        lifecycleScope.launch {
            // Remove old observers if set
            sectionDao.getSections().removeObservers(this@MainActivity)
            sectionDao.getSections().observe(this@MainActivity) { sections ->
                sections.forEach { section ->
                    addSectionToLayout(layout, section)
                }
            }
        }
    }

    private fun addSectionToLayout(layout: LinearLayout, section: Section) {
        if (layout.findViewWithTag<Button>(section.id.toString()) == null) {
            val button = Button(this, null, 0, R.style.SectionButtonStyle)
            button.tag = section.id.toString() // Use section ID as the tag
            button.text = section.sectionTitle
            button.setOnClickListener {
                val intent = Intent(this, SectionActivity::class.java)
                intent.putExtra("SECTION_ID", section.id)
                Log.d("MainActivity", "Section ID: ${section.id}")
                startActivity(intent)
            }

            // Add button to the layout
            layout.addView(button)

            // Include spacer by inflating it from the XML
            val inflater = layoutInflater
            val spacer = inflater.inflate(R.layout.spacer_20dp, layout, false)
            layout.addView(spacer)
        }
    }

}

class SectionActivity : AppCompatActivity() {
    private lateinit var dao: SectionDAO
    private lateinit var sectionTitleView: TextView
    private var sectionName: String? = null
    private var sectionId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section)

        val db = SectionDatabase(applicationContext)
        dao = db.getSectionDao()

        sectionTitleView = findViewById<TextView>(R.id.sectionTitleText)
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        if (sectionId == -1) {
            // Handle the error: ID not found
            finish()
            return
        }

        // Fetch section name asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            sectionName = dao.getSectionById(sectionId)?.sectionTitle
            withContext(Dispatchers.Main) {
                // Update the UI on the main thread
                sectionTitleView.text = sectionName ?: "Unknown Section"
            }
        }

        setupButtons()
    }

    private fun setupButtons() {
        val addTaskButton = findViewById<Button>(R.id.buttonAddNewTask)
        addTaskButton.setOnClickListener {
            showAddTaskDialog()  // Show dialog to add a new task
        }

        val editSectionButton = findViewById<ImageButton>(R.id.settingsButton)
        editSectionButton.setOnClickListener {
            sectionName?.let { name ->
                editSection(sectionTitleView, name, sectionId)  // Pass sectionId as needed for database operations
            }
        }

        val backToSectionButton = findViewById<ImageButton>(R.id.backButton)
        backToSectionButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Go back to the previous Activity
        }
    }

    private fun editSection(sectionView: View, originalSectionName: String?, sectionId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Section Name")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(originalSectionName)
        builder.setView(input)

        builder.setPositiveButton("Apply") { dialog, which ->
            val newSectionName = input.text.toString()
            val textViewSectionName = sectionView.findViewById<TextView>(R.id.sectionTitleText)
            textViewSectionName.text = newSectionName

            // Asynchronously update the database
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateSectionTitle(sectionId, newSectionName)
            }
        }

        builder.setNegativeButton("Delete") { dialog, which ->
            dialog.cancel()
            showDeleteConfirmationDialog(sectionView, originalSectionName, sectionId)
        }

        builder.show()
    }

    private fun showDeleteConfirmationDialog(sectionView: View, sectionName: String?, sectionId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Are you sure you want to delete the section '$sectionName'? This action cannot be undone.")

        builder.setPositiveButton("Delete") { dialog, which ->
            // Asynchronously delete the section from the database
            lifecycleScope.launch {
                dao.deleteSectionById(sectionId)  // Assuming you have the section ID
                // After deletion, you might want to update the UI or return to the previous screen
                withContext(Dispatchers.Main) {
                    finish()  // Closes the current activity, should return to the previous activity which needs to refresh its data
                }
            }
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
