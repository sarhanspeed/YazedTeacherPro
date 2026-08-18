package com.sarhansoftware.yazedteacherpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sarhansoftware.yazedteacherpro.data.YazedTeacherProDb
import com.sarhansoftware.yazedteacherpro.ui.YazedTeacherProApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = YazedTeacherProDb(applicationContext)
        db.writableDatabase
        db.ensureTrialStarted()
        setContent { YazedTeacherProApp(db) }
    }
}
