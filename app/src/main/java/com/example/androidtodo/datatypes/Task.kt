package com.example.androidtodo.datatypes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import org.joda.time.DateTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE  // Specifies what action to take on delete
        )
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val sectionId: Int,
    val taskDescription: String,
    val taskDueDate: String,
    val createdAt: DateTime = DateTime.now(),
    val editedAt: DateTime = DateTime.now(),
    val taskDone: Boolean = false
)