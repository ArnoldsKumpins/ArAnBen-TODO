package com.example.androidtodo.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.androidtodo.datatypes.Section


@Database(entities = [Section::class], version = 1)
abstract class SectionDatabase :RoomDatabase(){

    abstract fun getSectionDao() : SectionDAO

    companion object{
        @Volatile
        private var instance: SectionDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?:
        synchronized(LOCK){
            instance ?:
            createDatabase(context).also{
                instance = it
            }
        }

        private fun createDatabase(context: Context)=
            Room.databaseBuilder(
                context.applicationContext,
                SectionDatabase::class.java,
                "tododb"
            ).build()

    }
}