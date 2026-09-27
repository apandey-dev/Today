package com.minimal.today.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.minimal.today.util.IconHelper
import com.minimal.today.widget.WidgetHelper

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        IconHelper.updateDynamicIcon(context)
        WidgetHelper.updateAllWidgets(context)
    }
}
