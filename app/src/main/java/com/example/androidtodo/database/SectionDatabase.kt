package com.example.androidtodo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.androidtodo.datatypes.Section
import com.example.androidtodo.datatypes.Task

@Database(entities = [Section::class, Task::class], version = 1)
abstract class SectionDatabase : RoomDatabase() {

    abstract fun getSectionDao(): SectionDAO

    companion object {
        @Volatile
        private var instance: SectionDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also {
                instance = it
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                SectionDatabase::class.java,
                "tododb"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
