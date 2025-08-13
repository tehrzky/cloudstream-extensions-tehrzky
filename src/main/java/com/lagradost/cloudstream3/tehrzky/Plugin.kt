package com.lagradost.cloudstream3.yourname

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class DramaCoolPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DramaCoolProvider())
    }
}