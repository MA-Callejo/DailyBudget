package com.kiwi.finanzas.db

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kiwi.finanzas.R

@Database(entities = [Entrada::class, Tipo::class], version = 1)
abstract class DataBase : RoomDatabase() {
    abstract fun entryDao(): EntradaDAO
    abstract fun typeDao(): TipoDAO

    companion object {

        private fun createCallback(context: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val lloguer = context.getString(R.string.lloguer)
                val restaurants = context.getString(R.string.restaurants)
                val oci = context.getString(R.string.oci)
                val mascotes = context.getString(R.string.mascotes)
                val supermercat = context.getString(R.string.supermercat)
                val transport = context.getString(R.string.F)

                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$lloguer', 1, 0.4, 0.0, 0.0)")
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$restaurants', 1, 0.5, 0.2, 0.5)")
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$oci', 1, 0.3, 0.3, 0.8)")
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$mascotes', 1, 0.2, 0.5, 0.5)")
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$supermercat', 1, 0.3, 0.8, 0.3)")
                db.execSQL("INSERT INTO tipos (nombre, disponible, red, green, blue) VALUES ('$transport', 1, 0.8, 0.8, 0.2)")
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
                    .addCallback(createCallback(context)).build()
                INSTANCE = instance
                instance
            }
        }
    }
}