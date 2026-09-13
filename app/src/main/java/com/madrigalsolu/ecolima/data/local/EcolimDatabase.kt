package com.madrigalsolu.ecolima.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.madrigalsolu.ecolima.data.local.dao.RegistroDao
import com.madrigalsolu.ecolima.data.local.dao.ZonaDao
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import com.madrigalsolu.ecolima.data.local.entity.ZonaEntity

/**
 * Room database for ECOLIM S.A.C. offline-first storage.
 *
 * ### Why SQLite + sync (course requirement)
 * - Works offline in field operations (operarios sin conectividad).
 * - Transactional local writes with immediate UI feedback (Flow).
 * - Batch sync via WorkManager when connectivity returns.
 * - Server (REST API) remains system-of-record for analytics/long-term storage.
 *
 * Singleton access via [getInstance] or Hilt module.
 */
@Database(
    entities = [RegistroEntity::class, ZonaEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EcolimDatabase : RoomDatabase() {

    abstract fun registroDao(): RegistroDao
    abstract fun zonaDao(): ZonaDao

    companion object {
        const val DB_NAME = "ecolim.db"

        @Volatile
        private var INSTANCE: EcolimDatabase? = null

        fun getInstance(context: android.content.Context): EcolimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    EcolimDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Room type converters for enums.
 */
class Converters {

    @TypeConverter
    fun fromResiduoTipo(value: ResiduoTipo): String = value.name

    @TypeConverter
    fun toResiduoTipo(value: String): ResiduoTipo = ResiduoTipo.valueOf(value)

    @TypeConverter
    fun fromEstado(value: EstadoRegistro): String = value.name

    @TypeConverter
    fun toEstado(value: String): EstadoRegistro = EstadoRegistro.valueOf(value)
}
