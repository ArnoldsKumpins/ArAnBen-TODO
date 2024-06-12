package com.example.androidtodo.datatypes

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
// https://stackoverflow.com/questions/47511750/how-to-use-foreign-key-in-room-persistence-library
@Parcelize
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sectionId: Int,
    val taskDescription: String,
    val taskDueDate: String,
    val createdAt: String,
    var editedAt: String = DateTime.now().toString(),
    var taskDone: Boolean = false
) : Parcelable
