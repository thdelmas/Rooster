package com.rooster.rooster.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * iOS-style wheel picker for time selection
 * Provides smooth scrolling with deceleration and snapping to items
 */
class WheelTimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Properties
    private var items = (0..23).map { String.format("%02d", it) }
    private var selectedIndex = 0
    private var itemHeight = 0f
    private var visibleItems = 5
    
    // Painting
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    // Scrolling
    private var currentOffset = 0f
    private var scroller: Scroller = Scroller(context, DecelerateInterpolator())
    private var velocityTracker: VelocityTracker? = null
    private var lastTouchY = 0f
    private var isDragging = false
    private val minFlingVelocity = 50
    private val maxFlingVelocity = 8000
    
    // Callbacks
    var onValueChangedListener: ((Int) -> Unit)? = null
    
    init {
        // Load colors from theme
        val primaryColor = ContextCompat.getColor(context, R.color.md_theme_dark_primary)
        val onSurfaceColor = ContextCompat.getColor(context, R.color.md_theme_dark_onSurface)
        val onSurfaceVariantColor = ContextCompat.getColor(context, R.color.md_theme_dark_onSurfaceVariant)
        
        selectedTextPaint.color = primaryColor
        textPaint.color = onSurfaceVariantColor
        dividerPaint.color = onSurfaceColor
        
        // Parse custom attributes if provided
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.WheelTimePicker, 0, 0)
            try {
                visibleItems = typedArray.getInt(R.styleable.WheelTimePicker_visibleItems, 5)
                textPaint.textSize = typedArray.getDimension(
                    R.styleable.WheelTimePicker_textSize, 
                    60f
                )
                selectedTextPaint.textSize = textPaint.textSize * 1.2f
            } finally {
                typedArray.recycle()
            }
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        itemHeight = textPaint.textSize * 2f
        val height = (itemHeight * visibleItems).toInt()
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerY = height / 2f
        val centerX = width / 2f
        
        // Draw dividers
        val dividerTop = centerY - itemHeight / 2
        val dividerBottom = centerY + itemHeight / 2
        canvas.drawLine(0f, dividerTop, width.toFloat(), dividerTop, dividerPaint)
        canvas.drawLine(0f, dividerBottom, width.toFloat(), dividerBottom, dividerPaint)
        
        // Calculate scroll offset
        val offset = currentOffset % itemHeight
        val startIndex = (currentOffset / itemHeight).toInt()
        
        // Draw items
        for (i in -visibleItems / 2..visibleItems / 2 + 1) {
            val index = (startIndex + i + items.size * 100) % items.size
            val itemY = centerY + (i * itemHeight) - offset
            
            if (itemY < -itemHeight || itemY > height + itemHeight) continue
            
            // Calculate distance from center for scaling and alpha
            val distanceFromCenter = abs(itemY - centerY)
            val maxDistance = itemHeight * 2
            val scale = 1f - (distanceFromCenter / maxDistance).coerceIn(0f, 0.6f)
            val alpha = (255 * scale).toInt().coerceIn(50, 255)
            
            // Choose paint based on position
            val paint = if (distanceFromCenter < itemHeight / 2) {
                selectedTextPaint.apply { this.alpha = alpha }
            } else {
                textPaint.apply { this.alpha = alpha }
            }
            
            // Draw text
            val textY = itemY + paint.textSize / 3
            canvas.drawText(items[index], centerX, textY, paint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastTouchY = event.y
                isDragging = true
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaY = event.y - lastTouchY
                    currentOffset -= deltaY
                    
                    // Constrain offset
                    val maxOffset = items.size * itemHeight
                    while (currentOffset < 0) currentOffset += maxOffset
                    while (currentOffset >= maxOffset) currentOffset -= maxOffset
                    
                    lastTouchY = event.y
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    
                    if (abs(velocityY) > minFlingVelocity) {
                        fling(-velocityY.toInt())
                    } else {
                        snapToNearest()
                    }
                    
                    isDragging = false
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun fling(velocityY: Int) {
        scroller.fling(
            0, currentOffset.toInt(),
            0, velocityY,
            0, 0,
            Int.MIN_VALUE, Int.MAX_VALUE
        )
        invalidate()
    }
    
    private fun snapToNearest() {
        val nearest = (currentOffset / itemHeight).roundToInt() * itemHeight
        scroller.startScroll(
            0, currentOffset.toInt(),
            0, (nearest - currentOffset).toInt(),
            300
        )
        invalidate()
    }
    
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            currentOffset = scroller.currY.toFloat()
            
            // Wrap around
            val maxOffset = items.size * itemHeight
            while (currentOffset < 0) currentOffset += maxOffset
            while (currentOffset >= maxOffset) currentOffset -= maxOffset
            
            invalidate()
            
            if (scroller.isFinished) {
                onScrollFinished()
            }
        }
    }
    
    private fun onScrollFinished() {
        val newIndex = ((currentOffset / itemHeight).roundToInt() % items.size)
        if (newIndex != selectedIndex) {
            selectedIndex = newIndex
            onValueChangedListener?.invoke(selectedIndex)
        }
    }
    
    fun setItems(newItems: List<String>) {
        items = newItems
        invalidate()
    }
    
    fun setSelectedIndex(index: Int, animated: Boolean = false) {
        val targetOffset = index * itemHeight
        
        if (animated) {
            scroller.startScroll(
                0, currentOffset.toInt(),
                0, (targetOffset - currentOffset).toInt(),
                300
            )
            invalidate()
        } else {
            currentOffset = targetOffset
            selectedIndex = index
            invalidate()
        }
    }
    
    fun getSelectedIndex(): Int = selectedIndex
    
    fun getSelectedValue(): String = items[selectedIndex]
}












