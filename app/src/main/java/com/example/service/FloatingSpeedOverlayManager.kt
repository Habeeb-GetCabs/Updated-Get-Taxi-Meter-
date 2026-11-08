package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import java.util.Locale

/**
 * FloatingSpeedOverlayManager manages an authoritative real-time floating speed bubble
 * displayed over the Android Home screen and other applications while an active trip is running.
 *
 * Characteristics:
 * - Uses authoritative GPS speed data from LocationTrackingService (no secondary GPS tracker).
 * - Implemented using standard Android WindowManager (android.permission.SYSTEM_ALERT_WINDOW).
 * - Applies WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON to the visible floating overlay window
 *   while active and visible, and removes the flag cleanly when hidden, disabled, or removed.
 * - Checks Settings.canDrawOverlays(context) before attempting to show.
 * - Draggable by the user, smoothly updating position and persisting coordinates.
 * - Tap safely brings the Get Taxi app back to foreground.
 * - Thread-safe on Main Looper.
 * - Clean teardown when trip ends, leaves, or finishes.
 */
class FloatingSpeedOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingSpeedOverlay"
        private const val PREFS_NAME = "gotaxii_overlay_prefs"
        private const val KEY_POS_X = "overlay_pos_x"
        private const val KEY_POS_Y = "overlay_pos_y"
        private const val KEY_BUBBLE_ENABLED = "overlay_bubble_enabled"

        @Volatile
        private var overlayView: FrameLayout? = null
        @Volatile
        private var speedTextView: TextView? = null
        @Volatile
        private var unitTextView: TextView? = null
        @Volatile
        private var layoutParams: WindowManager.LayoutParams? = null
        @Volatile
        private var isShowing = false
        @Volatile
        private var lastSpeedKmh: Double = 0.0

        fun buildOverlayWindowFlags(keepScreenOn: Boolean = true): Int {
            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            if (keepScreenOn) {
                flags = flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            }
            return flags
        }

        fun isOverlayCurrentlyShowing(): Boolean = isShowing

        fun isOverlayKeepScreenOnActive(): Boolean {
            val params = layoutParams ?: return false
            val view = overlayView ?: return false
            return isShowing &&
                view.visibility == View.VISIBLE &&
                (params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        }

        fun getCurrentOverlayFlagsForTest(): Int {
            return layoutParams?.flags ?: 0
        }

        fun isBubbleEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_BUBBLE_ENABLED, true)
        }

        fun setBubbleEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply()
            if (!enabled) {
                forceHide(context)
            }
        }

        fun forceHide(context: Context) {
            val clearAction = Runnable {
                val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val viewToRemove = overlayView
                val paramsToClear = layoutParams
                if (paramsToClear != null) {
                    paramsToClear.flags = paramsToClear.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                }
                if (wm != null && viewToRemove != null) {
                    try {
                        if (paramsToClear != null && viewToRemove.isAttachedToWindow) {
                            wm.updateViewLayout(viewToRemove, paramsToClear)
                        }
                    } catch (_: Exception) {}
                    try {
                        wm.removeViewImmediate(viewToRemove)
                        Log.d(TAG, "Floating speed overlay force-removed and FLAG_KEEP_SCREEN_ON released")
                    } catch (_: Exception) {
                        try {
                            wm.removeView(viewToRemove)
                        } catch (_: Exception) {}
                    }
                }
                isShowing = false
                overlayView = null
                speedTextView = null
                unitTextView = null
                layoutParams = null
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                clearAction.run()
            } else {
                Handler(Looper.getMainLooper()).post(clearAction)
            }
        }
    }

    private val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOverlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_BUBBLE_ENABLED, true)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply()
        if (!enabled) {
            hide()
        }
    }

    /**
     * Shows the floating speed bubble ONLY if a trip has been explicitly started and is active,
     * the bubble toggle is enabled, and overlay permission is granted.
     */
    fun show() {
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            hide()
            return
        }
        if (!isOverlayEnabled()) {
            hide()
            return
        }
        if (!hasOverlayPermission()) {
            return
        }

        val showAction = Runnable {
            if (!LocationTrackingService.isTripActivelyRunningForBubble() || !isOverlayEnabled() || !hasOverlayPermission()) {
                hide()
                return@Runnable
            }
            if (windowManager == null) return@Runnable

            val currentView = overlayView
            val currentParams = layoutParams
            val isActuallyAttached = isShowing && currentView != null && currentView.isAttachedToWindow
            if (isActuallyAttached) {
                currentView?.visibility = View.VISIBLE
                if (currentParams != null &&
                    (currentParams.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0
                ) {
                    currentParams.flags = currentParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    try {
                        windowManager.updateViewLayout(currentView, currentParams)
                    } catch (_: Exception) {}
                }
                updateSpeedDisplay(lastSpeedKmh)
                return@Runnable
            }

            // If isShowing was true but the view is no longer attached, clean up stale reference first
            if (currentView != null) {
                try {
                    windowManager.removeViewImmediate(currentView)
                } catch (_: Exception) {}
                overlayView = null
                speedTextView = null
                unitTextView = null
                layoutParams = null
                isShowing = false
            }

            try {
                initOverlayView()
                val params = layoutParams ?: return@Runnable
                val view = overlayView ?: return@Runnable
                view.visibility = View.VISIBLE
                params.flags = buildOverlayWindowFlags(keepScreenOn = true)

                windowManager.addView(view, params)
                isShowing = true
                updateSpeedDisplay(lastSpeedKmh)
                Log.d(TAG, "Floating speed overlay shown with FLAG_KEEP_SCREEN_ON")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing floating speed overlay", e)
                isShowing = false
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showAction.run()
        } else {
            mainHandler.post(showAction)
        }
    }

    /**
     * Ensures the floating bubble is visible and attached ONLY when an active trip is running.
     */
    fun ensureShowing() {
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            hide()
            return
        }
        if (isOverlayEnabled() && hasOverlayPermission()) {
            show()
        }
    }

    /**
     * Updates the displayed speed in km/h (Float overload).
     */
    fun updateSpeed(speedKmh: Float) {
        updateSpeed(speedKmh.toDouble())
    }

    /**
     * Updates the displayed speed in km/h ONLY when a trip is actively running.
     */
    fun updateSpeed(speedKmh: Double) {
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            hide()
            return
        }
        lastSpeedKmh = speedKmh
        val currentView = overlayView
        val attached = isShowing && currentView != null && currentView.isAttachedToWindow
        if (!attached) {
            if (isOverlayEnabled() && hasOverlayPermission()) {
                show()
            }
            return
        }

        mainHandler.post {
            updateSpeedDisplay(speedKmh)
        }
    }

    /**
     * Completely removes and hides the floating speed bubble, releasing FLAG_KEEP_SCREEN_ON.
     */
    fun hide() {
        forceHide(context)
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun initOverlayView() {
        val density = context.resources.displayMetrics.density
        val sizePx = (58 * density).toInt()

        val root = FrameLayout(context).apply {
            // Circular badge with dark background and Get Taxi crimson border
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0F172A.toInt()) // Sleek dark slate
                setStroke((2 * density).toInt(), 0xFFE53935.toInt()) // Crimson red ring
            }
            background = backgroundDrawable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 8 * density
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val speedTv = TextView(context).apply {
            text = "0"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        val unitTv = TextView(context).apply {
            text = "km/h"
            setTextColor(0xFFEF4444.toInt()) // Red accent
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        container.addView(speedTv)
        container.addView(unitTv)
        root.addView(container)

        // WindowManager layout params with FLAG_KEEP_SCREEN_ON while visible
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val savedX = prefs.getInt(KEY_POS_X, screenWidth - sizePx - (16 * density).toInt())
        val savedY = prefs.getInt(KEY_POS_Y, (240 * density).toInt())

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            windowType,
            buildOverlayWindowFlags(keepScreenOn = true),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX.coerceIn(0, (screenWidth - sizePx).coerceAtLeast(0))
            y = savedY.coerceIn((40 * density).toInt(), (context.resources.displayMetrics.heightPixels - sizePx))
        }

        // Draggable touch listener with tap detection
        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isDragging = true
                        }
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(overlayView, currentParams)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Safe tap: Bring Get Taxi app to the foreground
                            try {
                                val launchIntent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    )
                                }
                                context.startActivity(launchIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error launching MainActivity from overlay tap", e)
                            }
                        } else {
                            // Persist last user drag position
                            prefs.edit()
                                .putInt(KEY_POS_X, currentParams.x)
                                .putInt(KEY_POS_Y, currentParams.y)
                                .apply()
                        }
                        return true
                    }
                }
                return false
            }
        })

        overlayView = root
        speedTextView = speedTv
        unitTextView = unitTv
        layoutParams = params
    }

    private fun updateSpeedDisplay(speedKmh: Double) {
        val displaySpeed = if (speedKmh <= 0.8) 0 else Math.round(speedKmh).toInt()
        speedTextView?.text = String.format(Locale.US, "%d", displaySpeed)
    }
}
