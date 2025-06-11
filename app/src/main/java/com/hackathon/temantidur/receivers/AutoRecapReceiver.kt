package com.hackathon.temantidur.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hackathon.temantidur.services.AiDailyRecapService

class AutoRecapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.hackathon.temantidur.ACTION_AUTO_RECAP") {
            AiDailyRecapService.enqueueWork(context, Intent())
        }
    }
}