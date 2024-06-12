package com.example.androidtodo.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidtodo.datatypes.Section
import com.example.androidtodo.datatypes.Task

@Dao
interface DbDAO {

//    https://medium.com/@barryalan2633/what-do-insert-update-delete-or-query-do-7e69683f9c68
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



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createTask(task: Task)

    @Query("UPDATE tasks SET taskDescription = :taskDescription, taskDueDate = :taskDueDate, editedAt = :editedAt WHERE id = :taskId")
    suspend fun updateTask1(taskDescription: String, taskDueDate: String, editedAt: String, taskId: Int)
    @Update
    suspend fun updateTask(task: Task)
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int): Int

    @Query("SELECT * FROM tasks WHERE sectionId = :sectionId ORDER BY createdAt DESC")
    fun getTasksForSection(sectionId: Int): LiveData<List<Task>>

}