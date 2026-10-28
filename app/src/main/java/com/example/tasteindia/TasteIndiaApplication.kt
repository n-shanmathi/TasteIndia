package com.example.tasteindia

import android.app.Application
import com.example.tasteindia.di.AppContainer
import com.example.tasteindia.di.DefaultAppContainer

class TasteIndiaApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}
