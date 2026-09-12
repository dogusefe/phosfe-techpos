package com.phosfe.bkmtechpos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.phosfe.bkmtechpos.BuildConfig
import com.phosfe.bkmtechpos.data.local.dao.BatchCycleDao
import com.phosfe.bkmtechpos.data.local.dao.DeliveryDebtDao
import com.phosfe.bkmtechpos.data.local.dao.ParameterRecordDao
import com.phosfe.bkmtechpos.data.local.dao.ParameterProjectionDao
import com.phosfe.bkmtechpos.data.local.dao.PaymentRecordDao
import com.phosfe.bkmtechpos.data.local.dao.SettlementReceiptDao
import com.phosfe.bkmtechpos.data.local.entity.BatchCycleEntity
import com.phosfe.bkmtechpos.data.local.entity.DeliveryDebtEntity
import com.phosfe.bkmtechpos.data.local.entity.ParameterRecordEntity
import com.phosfe.bkmtechpos.data.local.entity.ParameterProjectionEntity
import com.phosfe.bkmtechpos.data.local.entity.PaymentRecordEntity
import com.phosfe.bkmtechpos.data.local.entity.SettlementReceiptEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        PaymentRecordEntity::class,
        DeliveryDebtEntity::class,
        BatchCycleEntity::class,
        ParameterRecordEntity::class,
        ParameterProjectionEntity::class,
        SettlementReceiptEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TerminalDatabase : RoomDatabase() {
    abstract fun payments(): PaymentRecordDao
    abstract fun deliveryDebts(): DeliveryDebtDao
    abstract fun batches(): BatchCycleDao
    abstract fun parameters(): ParameterRecordDao
    abstract fun parameterProjections(): ParameterProjectionDao
    abstract fun settlements(): SettlementReceiptDao

    companion object {
        @Volatile private var instance: TerminalDatabase? = null

        fun open(context: Context): TerminalDatabase = instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

        private fun create(context: Context): TerminalDatabase {
            val builder = Room.databaseBuilder(context, TerminalDatabase::class.java, DATABASE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2)
            if (BuildConfig.DB_ENCRYPTED) {
                SQLiteDatabase.loadLibs(context)
                builder.openHelperFactory(SupportFactory(DatabasePassphraseStore(context).getOrCreate()))
            }
            return builder.build()
        }

        private const val DATABASE_NAME = "phosfe-techpos.db"
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS parameter_projection (type INTEGER NOT NULL, reference TEXT NOT NULL, version INTEGER NOT NULL, kind TEXT NOT NULL, summary TEXT NOT NULL, activatedAtEpochMs INTEGER NOT NULL, PRIMARY KEY(type))")
            }
        }
    }
}
