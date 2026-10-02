package com.rishabh.gymbros.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.min

internal object WidgetGraphics {
    private val primary = Color.parseColor("#38E89F")
    private val water = Color.parseColor("#4DE1FF")
    private val waterDeep = Color.parseColor("#1FB6D9")
    private val inactive = Color.parseColor("#262A40")
    private val bottleBorder = Color.parseColor("#222635")
    private val bottleBackground = Color.parseColor("#0A0B10")

    fun bottleBitmap(context: Context, progress: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (42f * density).toInt().coerceAtLeast(1)
        val height = (58f * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val stroke = 1.7f * density
        val capWidth = 16f * density
        val capHeight = 6f * density
        val bodyTop = 5f * density
        val bodyRect = RectF(
            stroke,
            bodyTop,
            width - stroke,
            height - stroke
        )
        val radius = 10f * density

        paint.style = Paint.Style.FILL
        paint.color = inactive
        val capLeft = (width - capWidth) / 2f
        canvas.drawRoundRect(
            RectF(capLeft, 0f, capLeft + capWidth, capHeight),
            2.5f * density,
            2.5f * density,
            paint
        )

        val bodyPath = Path().apply {
            addRoundRect(bodyRect, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(bodyPath)
        paint.style = Paint.Style.FILL
        paint.color = bottleBackground
        canvas.drawRect(bodyRect, paint)

        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            val fillTop = bodyRect.bottom - (bodyRect.height() * clamped)
            paint.shader = LinearGradient(
                0f,
                fillTop,
                0f,
                bodyRect.bottom,
                intArrayOf(water, waterDeep),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bodyRect.left, fillTop, bodyRect.right, bodyRect.bottom, paint)
            paint.shader = null

            val wave = Path().apply {
                moveTo(bodyRect.left, fillTop)
                val waveHeight = 2.2f * density
                val quarter = bodyRect.width() / 4f
                cubicTo(
                    bodyRect.left + quarter * 0.5f,
                    fillTop - waveHeight,
                    bodyRect.left + quarter * 1.5f,
                    fillTop + waveHeight,
                    bodyRect.left + quarter * 2f,
                    fillTop
                )
                cubicTo(
                    bodyRect.left + quarter * 2.5f,
                    fillTop - waveHeight,
                    bodyRect.left + quarter * 3.5f,
                    fillTop + waveHeight,
                    bodyRect.right,
                    fillTop
                )
                lineTo(bodyRect.right, fillTop + 3f * density)
                lineTo(bodyRect.left, fillTop + 3f * density)
                close()
            }
            paint.color = water
            canvas.drawPath(wave, paint)
        }
        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = bottleBorder
        canvas.drawRoundRect(bodyRect, radius, radius, paint)

        return bitmap
    }

    /** Full-card water fill used by the compact hydration widget. */
    fun compactWaterBitmap(context: Context, progress: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        // Render wide and let the RemoteViews ImageView scale it to the launcher cell.
        val width = (360f * density).toInt().coerceAtLeast(1)
        val height = (82f * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped <= 0f) return bitmap

        val fillTop = height - height * clamped
        val waveHeight = 3.5f * density
        val wavePath = Path().apply {
            moveTo(0f, fillTop)
            val quarter = width / 4f
            cubicTo(quarter * 0.5f, fillTop - waveHeight, quarter * 1.5f, fillTop + waveHeight, quarter * 2f, fillTop)
            cubicTo(quarter * 2.5f, fillTop - waveHeight, quarter * 3.5f, fillTop + waveHeight, width.toFloat(), fillTop)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }

        paint.shader = LinearGradient(
            0f, fillTop, 0f, height.toFloat(),
            intArrayOf(water, waterDeep), null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(wavePath, paint)
        paint.shader = null
        return bitmap
    }

    fun ringBitmap(context: Context, progress: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (78f * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val stroke = 7f * density
        paint.strokeWidth = stroke
        val inset = stroke / 2f + density
        val oval = RectF(inset, inset, size - inset, size - inset)

        paint.color = inactive
        canvas.drawArc(oval, -90f, 360f, false, paint)

        val sweep = 360f * progress.coerceIn(0f, 1f)
        if (sweep > 0f) {
            val center = size / 2f
            paint.shader = SweepGradient(
                center,
                center,
                intArrayOf(waterDeep, water, primary),
                floatArrayOf(0f, 0.55f, 1f)
            )
            canvas.drawArc(oval, -90f, min(sweep, 359.8f), false, paint)
            paint.shader = null
        }

        return bitmap
    }
}
