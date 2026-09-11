package com.phosfe.bkmtechpos

import android.app.Application
import com.phosfe.bkmtechpos.storage.PersistenceGraph

class PhosfeTechposApplication : Application() {
    lateinit var persistence: PersistenceGraph
        private set

    override fun onCreate() {
        super.onCreate()
        persistence = PersistenceGraph.create(this)
        persistence.database.openHelper.writableDatabase
    }
}
