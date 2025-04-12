package com.decade.practice

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.decade.practice.components.SHARED_PREFERENCES_NAME
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    private fun clean(context: Context) {
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .clear().commit()
        context.databaseList().forEach { dbName ->
            context.deleteDatabase(dbName)
        }
    }

    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        clean(context)
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
