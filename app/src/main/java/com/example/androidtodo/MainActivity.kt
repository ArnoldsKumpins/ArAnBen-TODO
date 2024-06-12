/*
####################################
   * Android CRUD TODO Application
   *
   * Created BY:
   *    Arnolds Kumpiņš 221RDB157
   *    Andris Martinsons 221RDB203
   *    Bernhards Arnītis 221RDB128
####################################
*/

package com.example.androidtodo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.app.AlertDialog
import android.content.Context
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
import android.view.ViewGroup
import android.widget.ImageButton
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.androidtodo.datatypes.Section
import com.example.androidtodo.datatypes.Task
import com.example.androidtodo.database.DbDAO
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidtodo.database.CrudDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

class SectionAdapter(private val context: Context, private val sections: List<Section>) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.section_item, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section)
    }

    override fun getItemCount(): Int {
        return sections.size
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionButton: Button = itemView.findViewById(R.id.sectionButton)

        fun bind(section: Section) {
            sectionButton.text = section.sectionTitle

            sectionButton.setOnClickListener {
                Log.d("SectionAdapter", "Clicked on section: ${section.sectionTitle}, ID: ${section.id}")
                val intent = Intent(context, TaskActivity::class.java)
                intent.putExtra("SECTION_ID", section.id)
                context.startActivity(intent)
            }
        }
    }
}

class TaskAdapter(private val context: Context, private var tasks: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val dao = CrudDatabase(context).getSectionDao()
    private var filteredTasks: List<Task> = tasks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = filteredTasks[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int {
        return filteredTasks.size
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        filteredTasks = newTasks
        notifyDataSetChanged()
    }

    fun filterTasks(query: String) {
        filteredTasks = if (query.isEmpty()) {
            tasks
        } else {
            tasks.filter { task ->
                task.taskDescription.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    private fun updateTaskDoneStatus(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.updateTask(task)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskDescriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val taskDueDateTextView: TextView = itemView.findViewById(R.id.textViewDueDate)
        private val editTaskButton: ImageView = itemView.findViewById(R.id.taskEdit)
        private val deleteTaskButton: ImageView = itemView.findViewById(R.id.taskDelete)
        private val taskDoneCheckBox: CheckBox = itemView.findViewById(R.id.taskDoneCheckBox)

        fun bind(task: Task) {
            taskDescriptionTextView.text = task.taskDescription
            taskDueDateTextView.text = task.taskDueDate
            taskDoneCheckBox.isChecked = task.taskDone


            editTaskButton.setOnClickListener {
                if (context is TaskActivity) {
                    context.editTask(task)
                }
            }

            deleteTaskButton.setOnClickListener {
                if (context is TaskActivity) {
                    context.showDeleteTaskConfirmationDialog(itemView, task)
                }
            }

            if (task.taskDone) {
                taskDescriptionTextView.paintFlags = taskDescriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskDescriptionTextView.alpha = 0.5f
            } else {
                taskDescriptionTextView.paintFlags = taskDescriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskDescriptionTextView.alpha = 1.0f
            }

            taskDoneCheckBox.setOnCheckedChangeListener { _, isChecked ->
                task.taskDone = isChecked
                task.editedAt = DateTime.now().toString()
                updateTaskDoneStatus(task)

                if (isChecked) {
                    taskDescriptionTextView.paintFlags = taskDescriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    taskDescriptionTextView.alpha = 0.5f
                } else {
                    taskDescriptionTextView.paintFlags = taskDescriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    taskDescriptionTextView.alpha = 1.0f
                }
            }
        }
    }
}


class MainActivity : AppCompatActivity() {
    private lateinit var sectionDao: DbDAO
    private lateinit var sectionAdapter: SectionAdapter
    private val sections = mutableListOf<Section>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = CrudDatabase(applicationContext)
        sectionDao = db.getSectionDao()

        val recyclerView = findViewById<RecyclerView>(R.id.sectionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        sectionAdapter = SectionAdapter(this, sections)
        recyclerView.adapter = sectionAdapter

        loadSections()

        val addSectionButton = findViewById<Button>(R.id.buttonAddNewSection)
        addSectionButton.setOnClickListener {
            showAddSectionDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSections()
    }

//    https://stackoverflow.com/questions/52076779/kotlin-custom-dialog-in-android
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
            sectionDao.createSection(Section(sectionTitle = sectionName))
            loadSections()
        }
    }

    private fun loadSections() {
        lifecycleScope.launch {
            sectionDao.getSections().observe(this@MainActivity) { updatedSections ->
                sections.clear()
                sections.addAll(updatedSections)
                sectionAdapter.notifyDataSetChanged()
            }
        }
    }
}

class TaskActivity : AppCompatActivity() {
    private lateinit var dao: DbDAO
    private lateinit var taskAdapter: TaskAdapter
    private val tasks = mutableListOf<Task>()
    private lateinit var sectionTitleView: TextView
    private var sectionName: String? = null
    private var sectionId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        val db = CrudDatabase(applicationContext)
        dao = db.getSectionDao()

        val searchBar = findViewById<EditText>(R.id.search_bar)
        val recyclerView = findViewById<RecyclerView>(R.id.taskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(this, tasks)
        recyclerView.adapter = taskAdapter

        sectionTitleView = findViewById(R.id.sectionTitleText)
        sectionId = intent.getIntExtra("SECTION_ID", -1)
        if (sectionId == -1) {
            finish()
            return
        }

        loadTasks()

        CoroutineScope(Dispatchers.IO).launch {
            sectionName = dao.getSectionById(sectionId)?.sectionTitle
            withContext(Dispatchers.Main) {
                sectionTitleView.text = sectionName ?: "Unknown Section"
            }
        }

        setupButtons()

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                taskAdapter.filterTasks(query)
            }
        })
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            dao.getTasksForSection(sectionId).observe(this@TaskActivity) { updatedTasks ->
                tasks.clear()
                tasks.addAll(updatedTasks)
                taskAdapter.updateTasks(updatedTasks)
            }
        }
    }

    private fun setupButtons() {
        val addTaskButton = findViewById<Button>(R.id.buttonAddNewTask)
        addTaskButton.setOnClickListener {
            showAddTaskDialog(sectionId)
        }

        val editSectionButton = findViewById<ImageButton>(R.id.settingsButton)
        editSectionButton.setOnClickListener {
            sectionName?.let { name ->
                editSection(sectionTitleView, name, sectionId)
            }
        }

        val backToSectionButton = findViewById<ImageButton>(R.id.backButton)
        backToSectionButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun showAddTaskDialog(sectionId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Task")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_task_dialog, null)
        val taskInput = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)
        val dateInput = dialogView.findViewById<EditText>(R.id.editTextDueDate)

        builder.setView(dialogView)

        builder.setPositiveButton("Add", null)
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()

        val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        addButton.isEnabled = false

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

        addButton.setOnClickListener {
            val taskText = taskInput.text.toString()
            val dueDate = dateInput.text.toString()
            if (isValidDate(dueDate)) {
                addTask(taskText, dueDate, sectionId)
                dialog.dismiss()
            } else {
                dateInput.error = "Invalid date format"
            }
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
            lifecycleScope.launch {
                dao.deleteSectionById(sectionId)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun isValidDate(date: String): Boolean {
        val dateFormats = listOf(
            SimpleDateFormat("ddMMyyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        )
        return dateFormats.any { format ->
            try {
                format.isLenient = false

                val expectedLength = format.toPattern().replace("/", "").length
                if (date.replace("/", "").length != expectedLength) {
                    return@any false
                }

                format.parse(date)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun addTask(taskDescription: String, taskDueDate: String, sectionId: Int) {
        lifecycleScope.launch {
            dao.createTask(Task(taskDescription = taskDescription, taskDueDate = taskDueDate, createdAt = DateTime.now().toString(),sectionId = sectionId))
            loadTasks()
        }
    }

    fun editTask(task: Task) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Task")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_task_dialog, null)
        val inputDescription = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)
        val inputDueDate = dialogView.findViewById<EditText>(R.id.editTextDueDate)

        inputDescription.setText(task.taskDescription)
        inputDueDate.setText(task.taskDueDate)
        builder.setView(dialogView)

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        saveButton.isEnabled = false

        val initialDate = inputDueDate.text.toString()
        if (isValidDate(initialDate)) {
            saveButton.isEnabled = true
        }

        inputDueDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val dateText = inputDueDate.text.toString()
                if (isValidDate(dateText)) {
                    inputDueDate.error = null
                    saveButton.isEnabled = true
                } else {
                    inputDueDate.error = "Invalid date format"
                    saveButton.isEnabled = false
                }
            }
        })

        saveButton.setOnClickListener {
            val newDescription = inputDescription.text.toString()
            val newDueDate = inputDueDate.text.toString()

            if (isValidDate(newDueDate)) {
                lifecycleScope.launch {
                    dao.updateTask1(newDescription, newDueDate, DateTime.now().toString(), task.id)
                    loadTasks()
                    dialog.dismiss()
                }
            } else {
                inputDueDate.error = "Invalid date format"
            }
        }
    }


    fun showDeleteTaskConfirmationDialog(taskView: View, task: Task) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Are you sure you want to delete the task '${task.taskDescription}'? This action cannot be undone.")

        builder.setPositiveButton("Delete") { dialog, _ ->
            lifecycleScope.launch {
                dao.deleteTaskById(task.id)
                loadTasks()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }


}

