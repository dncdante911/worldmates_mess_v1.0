package com.worldmates.messenger

import android.app.Application
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import timber.log.Timber

/**
 * Главный Application класс WorldMates Messenger
 */
class WMApplication : MultiDexApplication() {

    companion object {
        private const val TAG = "WMApplication"
        lateinit var instance: WMApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }

        Log.d(TAG, "WorldMates Messenger Application started")
    }
}
