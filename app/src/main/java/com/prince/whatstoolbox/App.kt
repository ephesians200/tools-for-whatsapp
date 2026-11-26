package com.prince.whatstoolbox
import android.app.Application
import com.prince.whatstoolbox.util.Out
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp class App : Application() {
    @javax.inject.Inject lateinit var billing: com.prince.whatstoolbox.data.BillingManager
    override fun onCreate() { super.onCreate(); billing.start(); Thread { runCatching { Out.cleanup(this) } }.start() }   // drop stale exports / temp audio so the cache can't grow forever
}
