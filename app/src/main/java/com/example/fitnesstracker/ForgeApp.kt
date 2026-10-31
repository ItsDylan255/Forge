package com.example.fitnesstracker

import android.app.Application
import com.example.fitnesstracker.i18n.L10n

/** Lädt die gewählte Sprache, bevor Activity, Dienste oder Worker Texte erzeugen. */
class ForgeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        L10n.init(this)
    }
}
