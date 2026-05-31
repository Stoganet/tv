package com.stoganet.tv.di

import android.content.Context

class ServiceLocator(context: Context) {
    @Suppress("unused")
    private val appContext: Context = context.applicationContext
}
