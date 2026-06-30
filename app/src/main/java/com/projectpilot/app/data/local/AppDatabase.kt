package com.projectpilot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.projectpilot.app.domain.model.Project
import com.projectpilot.app.domain.model.ProjectType

class Converters {
    @TypeConverter fun fromType(t: ProjectType): String = t.name
    @TypeConverter fun toType(s: String): ProjectType =
        runCatching { ProjectType.valueOf(s) }.getOrDefault(ProjectType.UNKNOWN)
}

@Database(
    entities = [Project::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object { const val NAME = "projectpilot.db" }
}
