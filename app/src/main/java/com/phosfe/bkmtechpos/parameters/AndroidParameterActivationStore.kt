package com.phosfe.bkmtechpos.parameters

import android.content.Context
import com.phosfe.bkmtechpos.storage.PersistenceGraph

object AndroidParameterActivationStore {
    fun create(context: Context): ParameterActivationStore = PersistenceGraph.create(context).parameters
}
