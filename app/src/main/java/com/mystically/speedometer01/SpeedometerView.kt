package com.mystically.speedometer01

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min


class SpeedometerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val segmentPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 70f
        isAntiAlias = true
    }

    private val encasementPaint = Paint().apply {
        color = Color.parseColor("#757575")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 360f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val otherTextPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 120f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var currentSpeed: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var maxSpeed = 40f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2 - 100
        val centerX = width / 2
        val centerY = height / 2

        drawSegments(canvas, centerX, centerY, radius)
        drawEncasementCircles(canvas, centerX, centerY, radius)

        val typeface = resources.getFont(R.font.poppinsregular)
        textPaint.typeface = typeface

        canvas.drawText(currentSpeed.toInt().toString(), centerX, centerY + 40, textPaint)
        canvas.drawText("km/h", centerX, centerY + 250, otherTextPaint)
    }

    private fun drawSegments(canvas: Canvas?, centerX: Float, centerY: Float, radius: Float) {
        val totalSegments = 40
        val sweepAngle = 360f / totalSegments
        val startAngle = 90f
        val currentSegment = ((currentSpeed / maxSpeed * totalSegments).toInt() - 1)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)

        for (i in 0 until totalSegments) {

            segmentPaint.color = when {
                i < currentSegment -> ContextCompat.getColor(context, typedValue.resourceId)
                i == currentSegment -> Color.WHITE
                i == currentSegment + 1 && currentSegment > 0 -> Color.parseColor("#444444")
                else -> Color.parseColor("#222222")
            }

            val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            canvas?.drawArc(oval, startAngle + i * sweepAngle, sweepAngle - 4, false, segmentPaint) // Reduced gap between segments
        }
    }

    private fun drawEncasementCircles(canvas: Canvas?, centerX: Float, centerY: Float, radius: Float) {
        canvas?.drawCircle(centerX, centerY, radius + 50, encasementPaint) // Slightly larger than the segment radius
        canvas?.drawCircle(centerX, centerY, radius - 50, encasementPaint) // Slightly smaller than the segment radius
    }

    fun setMaxSpeed(value: String) {
        maxSpeed = value.toFloat()
        invalidate()
    }
}


