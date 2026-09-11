package com.phosfe.bkmtechpos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.phosfe.bkmtechpos.BuildConfig
import com.phosfe.bkmtechpos.data.local.dao.BatchCycleDao
import com.phosfe.bkmtechpos.data.local.dao.DeliveryDebtDao
import com.phosfe.bkmtechpos.data.local.dao.ParameterRecordDao
import com.phosfe.bkmtechpos.data.local.dao.PaymentRecordDao
import com.phosfe.bkmtechpos.data.local.dao.SettlementReceiptDao
import com.phosfe.bkmtechpos.data.local.entity.BatchCycleEntity
import com.phosfe.bkmtechpos.data.local.entity.DeliveryDebtEntity
import com.phosfe.bkmtechpos.data.local.entity.ParameterRecordEntity
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
        SettlementReceiptEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TerminalDatabase : RoomDatabase() {
    abstract fun payments(): PaymentRecordDao
    abstract fun deliveryDebts(): DeliveryDebtDao
    abstract fun batches(): BatchCycleDao
    abstract fun parameters(): ParameterRecordDao
    abstract fun settlements(): SettlementReceiptDao

    companion object {
        @Volatile private var instance: TerminalDatabase? = null

        fun open(context: Context): TerminalDatabase = instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

        private fun create(context: Context): TerminalDatabase {
            val builder = Room.databaseBuilder(context, TerminalDatabase::class.java, DATABASE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            if (BuildConfig.DB_ENCRYPTED) {
                SQLiteDatabase.loadLibs(context)
                builder.openHelperFactory(SupportFactory(DatabasePassphraseStore(context).getOrCreate()))
            }
            return builder.build()
        }

        private const val DATABASE_NAME = "phosfe-techpos.db"
    }
}
