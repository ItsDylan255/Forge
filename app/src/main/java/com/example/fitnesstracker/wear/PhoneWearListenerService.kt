package com.example.fitnesstracker.wear

import com.example.fitnesstracker.service.LiveSync
import com.example.fitnesstracker.service.RestTimerStore
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Nimmt die Knöpfe der Uhr entgegen: Pause verlängern oder überspringen. */
class PhoneWearListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        LiveSync.start(this)
        when (event.path) {
            WearBridge.PATH_REST_EXTEND -> RestTimerStore.extend(WearBridge.EXTEND_SECONDS)
            WearBridge.PATH_REST_SKIP -> RestTimerStore.skip()
        }
    }
}
