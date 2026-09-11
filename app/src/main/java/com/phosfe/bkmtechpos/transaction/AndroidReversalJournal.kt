package com.phosfe.bkmtechpos.transaction

import android.content.Context
import com.phosfe.bkmtechpos.storage.PersistenceGraph

object AndroidReversalJournal {
    fun create(context: Context): ReversalJournal = PersistenceGraph.create(context).reversals
}
