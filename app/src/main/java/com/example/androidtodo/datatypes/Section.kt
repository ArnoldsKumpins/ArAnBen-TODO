package com.example.androidtodo.datatypes

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime

@Entity(tableName = "sections")
@Parcelize
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val createdAt: String = DateTime.now().toString(),
    val sectionTitle: String
) : Parcelable
