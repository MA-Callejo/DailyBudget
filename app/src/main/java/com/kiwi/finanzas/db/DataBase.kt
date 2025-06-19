package com.kiwi.finanzas.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Entrada::class, Tipo::class], version = 1)
abstract class DataBase : RoomDatabase() {
    abstract fun entryDao(): EntradaDAO
    abstract fun typeDao(): TipoDAO

    companion object {

        private val callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Lloguer', 1, 0.4, 0.0, 0.0)") // Rojo
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Restaurants', 1, 0.5, 0.2, 0.5)") // Morado
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Oci', 1, 0.3, 0.3, 0.8)") // Azul
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Mascotes', 1, 0.2, 0.5, 0.5)") // Amarillo
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Supermercat', 1, 0.3, 0.8, 0.3)") // Verde
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('Transport', 1, 0.8, 0.8, 0.2)") // Naranja
            }
        }
        @Volatile
        private var INSTANCE: DataBase? = null
        /*val MIGRATION_1_2 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tipos ADD COLUMN disponible INTEGER NOT NULL DEFAULT 1")
            }
        }*/

        fun getDatabase(context: Context): DataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DataBase::class.java,
                    "app_database"
                )//.addMigrations(MIGRATION_1_2)
                    .addCallback(callback).build()
                INSTANCE = instance
                instance
            }
        }
    }
}