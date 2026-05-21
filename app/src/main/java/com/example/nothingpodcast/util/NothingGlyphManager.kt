package com.example.nothingpodcast.util

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import com.nothing.ketchum.GlyphManager.Callback
import com.nothing.ketchum.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

@Singleton
class NothingGlyphManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var glyphManager: GlyphManager? = null
    private val glyphScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var downloadAnimationJob: Job? = null
    private var isInitialized = false
    private var isSessionOpen = false

    private val callback = object : Callback {
        override fun onServiceConnected(componentName: android.content.ComponentName?) {
            try {
                // Detect device and register correctly
                val deviceConstant = when {
                    Common.is20111() -> com.nothing.ketchum.Glyph.DEVICE_20111
                    Common.is22111() -> com.nothing.ketchum.Glyph.DEVICE_22111
                    Common.is23111() -> com.nothing.ketchum.Glyph.DEVICE_23111
                    Common.is23113() -> com.nothing.ketchum.Glyph.DEVICE_23113
                    Common.is24111() -> com.nothing.ketchum.Glyph.DEVICE_24111
                    else -> com.nothing.ketchum.Glyph.DEVICE_23111 // Default to 2a if unsure as user has it
                }
                
                glyphManager?.register(deviceConstant)
                isInitialized = true
                AppLogger.log(context, "INFO", "Glyph SDK Connected for device: $deviceConstant")
            } catch (e: Exception) {
                AppLogger.log(context, "ERROR", "Glyph Register Error: ${e.message}")
            }
        }

        override fun onServiceDisconnected(componentName: android.content.ComponentName?) {
            isInitialized = false
            isSessionOpen = false
            AppLogger.log(context, "INFO", "Glyph SDK Disconnected")
        }
    }

    init {
        try {
            glyphManager = GlyphManager.getInstance(context)
            glyphManager?.init(callback)
        } catch (e: Exception) {
            AppLogger.log(context, "ERROR", "Glyph Init Error: ${e.message}")
        }
    }

    fun openSession() {
        if (!isInitialized || isSessionOpen) return
        try {
            android.util.Log.d("NothingGlyphManager", "Opening session...")
            glyphManager?.openSession()
            isSessionOpen = true
        } catch (e: Exception) {
            android.util.Log.e("NothingGlyphManager", "Error opening session", e)
        }
    }

    fun closeSession() {
        if (!isSessionOpen) return
        try {
            glyphManager?.closeSession()
            isSessionOpen = false
        } catch (e: Exception) {}
    }

    /**
     * Show progress (level 0-100) on the circular/segmented LED
     */
    fun showProgressTemporarily(level: Int) {
        if (!isInitialized) return
        try {
            android.util.Log.d("NothingGlyphManager", "Showing manual progressive seek: $level")
            openSession()
            val builder = glyphManager?.getGlyphFrameBuilder() ?: return
            
            // On Phone (2a), C arc has many segments. 
            // We'll try to light up C based on level.
            // Since displayProgress fails, we'll use a "pulse" of C for now 
            // but we'll also light up A and B to ensure visual feedback.
            builder.buildChannelA()
            builder.buildChannelB()
            builder.buildChannelC()
            
            glyphManager?.toggle(builder.build())
            
            // Auto turn off after 2 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val offBuilder = glyphManager?.getGlyphFrameBuilder() ?: return@postDelayed
                    glyphManager?.toggle(offBuilder.build())
                } catch (e: Exception) {}
            }, 2000)
        } catch (e: Exception) {}
    }

    /**
     * Double flash proportional to selected playback speed
     */
    fun pulseSpeedAction(speed: Float) {
        if (!isInitialized) return
        try {
            android.util.Log.d("NothingGlyphManager", "Pulse speed action: $speed")
            openSession()
            val builder = glyphManager?.getGlyphFrameBuilder() ?: return
            builder.buildChannelA()
            builder.buildChannelB()
            builder.buildChannelC()
            
            // Duration inversely proportional to speed
            val duration = (400 / speed).toInt().coerceIn(100, 1000)
            builder.buildPeriod(duration)
            val frame = builder.build()
            
            // First flash
            glyphManager?.animate(frame)
            
            // Second flash after first one + small gap
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { 
                    openSession() // Ensure session still open
                    glyphManager?.animate(frame) 
                } catch (e: Exception) {}
            }, (duration * 1.5).toLong())
            
            // Just clear LEDs, don't close session
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { 
                    val offBuilder = glyphManager?.getGlyphFrameBuilder() ?: return@postDelayed
                    glyphManager?.toggle(offBuilder.build())
                } catch (e: Exception) {}
            }, (duration * 3).toLong())
        } catch (e: Exception) {}
    }

    fun startDownloadAnimation() {
        if (!isInitialized) return
        stopDownloadAnimation()
        
        downloadAnimationJob = glyphScope.launch {
            openSession()
            var step = 0
            while (isActive) {
                try {
                    val builder = glyphManager?.getGlyphFrameBuilder() ?: break
                    when (step % 3) {
                        0 -> builder.buildChannelA()
                        1 -> builder.buildChannelB()
                        2 -> builder.buildChannelC()
                    }
                    step++
                    builder.buildPeriod(150)
                    glyphManager?.animate(builder.build())
                } catch (e: Exception) {
                    android.util.Log.e("NothingGlyphManager", "Error in download animation step", e)
                }
                delay(200)
            }
        }
    }

    fun stopDownloadAnimation() {
        downloadAnimationJob?.cancel()
        downloadAnimationJob = null
        try {
            val offBuilder = glyphManager?.getGlyphFrameBuilder() ?: return
            glyphManager?.toggle(offBuilder.build())
        } catch (e: Exception) {}
    }

    /**
     * All LEDs on for 2.5 seconds to signal completion
     */
    fun showDownloadComplete() {
        if (!isInitialized) return
        try {
            openSession()
            val builder = glyphManager?.getGlyphFrameBuilder() ?: return
            builder.buildChannelA()
            builder.buildChannelB()
            builder.buildChannelC()
            
            glyphManager?.toggle(builder.build())
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val offBuilder = glyphManager?.getGlyphFrameBuilder() ?: return@postDelayed
                    glyphManager?.toggle(offBuilder.build())
                } catch (e: Exception) {}
            }, 2500)
        } catch (e: Exception) {}
    }

    /**
     * Brief pulse for chapter changes or actions (Play/Pause)
     */
    fun pulseAction() {
        if (!isInitialized) return
        try {
            android.util.Log.d("NothingGlyphManager", "Pulse action triggered")
            openSession()
            val builder = glyphManager?.getGlyphFrameBuilder() ?: return
            builder.buildChannelA()
            builder.buildChannelB()
            builder.buildChannelC()
            builder.buildPeriod(800)
            glyphManager?.animate(builder.build())
            
            // Just clear LEDs, don't close session
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { 
                    val offBuilder = glyphManager?.getGlyphFrameBuilder() ?: return@postDelayed
                    glyphManager?.toggle(offBuilder.build())
                } catch (e: Exception) {}
            }, 1000)
        } catch (e: Exception) {}
    }
}
