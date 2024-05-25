package com.example.androidtodo.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidtodo.datatypes.Section

@Dao
interface SectionDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createSection(section: Section)


    @Update
    suspend fun updateSection(section: Section)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun deleteSectionById(sectionId: Int): Int

    @Query("SELECT * FROM sections ORDER BY createdAt DESC")
    fun getSections() : LiveData<List<Section>>

    @Query("UPDATE sections SET sectionTitle = :sectionTitle WHERE id = :sectionId")
    suspend fun updateSectionTitle(sectionId: Int, sectionTitle: String)

    @Query("SELECT * FROM sections WHERE id = :sectionId")
    fun getSectionById(sectionId: Int): Section?

}