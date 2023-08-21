package de.nif.zoomview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap



class ZoomView(mContext: Context, attrs : AttributeSet?, defStyleRes : Int) : AppCompatImageView(mContext, attrs, defStyleRes){

    constructor(mContext: Context, attrs: AttributeSet?) : this(mContext, attrs, 0 )
    constructor(mContext: Context) : this(mContext, null, 0 )

    companion object {
        const val SCALE_CENTER = 0
        const val SCALE_FIT_CENTER = 1
    }



    var minScale = 1.0f
    var maxScale = 6.0f

    var isScrollLimited = true
    var isScalable = true
    var isScrollable = true
    var scaleMode = SCALE_FIT_CENTER

    private val MODE_DEFAULT = 0
    private val MODE_SCALING = 1
    private val MODE_SCROLLING = 2

    private var mode : Int = MODE_DEFAULT

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, SimpleGestureListener())


    init {
        scaleType = ScaleType.MATRIX

    }


    fun setVectorDrawable(resId : Int) {

        val vectorDrawable = ResourcesCompat.getDrawable(
            context.resources,
            resId,
            context.theme
        )

        val bitmap = vectorDrawable?.toBitmap(
            300,
            300,
            Bitmap.Config.ARGB_8888
        )?.copy(Bitmap.Config.ARGB_8888, true)

        setImageBitmap(bitmap)

  }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        bm?.let {
            applyDefaultMatrix()
            postInvalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        applyDefaultMatrix()
        postInvalidate()

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        event?.let{e ->
            mode = MODE_DEFAULT

            if (isScalable)
                scaleGestureDetector.onTouchEvent(e)

            if (mode != MODE_SCALING) {
                gestureDetector.onTouchEvent(e)

            }

        }
        return true
    }

    private fun scaleImage(dScaleFactor : Float) {

        val px = measuredWidth / 2f
        val py = measuredHeight / 2f
        imageMatrix.postScale(dScaleFactor, dScaleFactor, px,  py)

        val mValues = FloatArray(9)
        imageMatrix.getValues(mValues)

        val scaleX = mValues[Matrix.MSCALE_X]
        val factor =
            if (scaleX > maxScale) maxScale / scaleX
            else if (scaleX < minScale) minScale / scaleX
            else 1f

        if (factor != 1f)
            imageMatrix.postScale(factor, factor, px,  py)

        applyScrollLimits()
        postInvalidate()

    }

    private fun applyDefaultMatrix() {

        drawable ?: return

        val bmWidth = drawable.intrinsicWidth
        val bmHeight = drawable.intrinsicHeight

        when (scaleMode) {
            SCALE_CENTER ->  imageMatrix = Matrix().apply {
                setTranslate((measuredWidth - bmWidth) * .5f, (measuredHeight - bmHeight) * .5f)
            }

            SCALE_FIT_CENTER ->  imageMatrix = Matrix().apply {
                setTranslate((measuredWidth - bmWidth) * .5f, (measuredHeight - bmHeight) * .5f)
                val factorX = measuredWidth.toFloat()/bmWidth
                val factorY = measuredHeight.toFloat()/bmHeight
                val scaleFactor = factorX.coerceAtMost(factorY)
                postScale(scaleFactor, scaleFactor, measuredWidth / 2f, measuredHeight / 2f)
            }
        }

    }

    private fun applyScrollLimits() {

        if (!isScrollLimited)
            return

        val mValues = FloatArray(9)
        imageMatrix.getValues(mValues)

        val scaledWidth = drawable.intrinsicWidth * mValues[Matrix.MSCALE_X]
        val scaledHeight = drawable.intrinsicWidth * mValues[Matrix.MSCALE_Y]

        val right = mValues[Matrix.MTRANS_X] + scaledWidth
        val bottom = mValues[Matrix.MTRANS_Y] + scaledHeight

        var dx = 0f
        var dy = 0f

        if (scaledWidth > measuredWidth) {

            dx =
                if (mValues[Matrix.MTRANS_X] > 0) -mValues[Matrix.MTRANS_X]
                else if (right < measuredWidth)  measuredWidth - right
                else dx

        } else if (scaledWidth < measuredWidth ) {

            dx =
                if (mValues[Matrix.MTRANS_X] < 0) -mValues[Matrix.MTRANS_X]
                else if (right > measuredWidth)  measuredWidth - right
                else dx
        }

        if (scaledHeight > measuredHeight) {

            dy =
                if (mValues[Matrix.MTRANS_Y] > 0) -mValues[Matrix.MTRANS_Y]
                else if (bottom < measuredHeight)  measuredHeight - bottom
                else dy


        } else if (scaledHeight < measuredHeight ) {

            dy =
                if (mValues[Matrix.MTRANS_Y] < 0) -mValues[Matrix.MTRANS_Y]
                else if (bottom > measuredHeight)  measuredHeight - bottom
                else dy

        }

        imageMatrix.postTranslate(dx, dy)

    }

    private fun scrollImage(dx: Float, dy: Float) {

        imageMatrix.postTranslate(-dx, -dy)
        applyScrollLimits()
        postInvalidate()

    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            scaleImage(detector.scaleFactor)

            return super.onScale(detector)
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = MODE_SCALING
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mode = MODE_DEFAULT
            super.onScaleEnd(detector)
        }

    }

    private inner class SimpleGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            dx: Float,
            dy: Float
        ): Boolean {
            if (isScrollable) {
                mode = MODE_SCROLLING
                scrollImage(dx, dy)
            }
            return super.onScroll(e1, e2, dx, dy)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mode == MODE_DEFAULT) {
                applyDefaultMatrix()
                postInvalidate()
            }
            return super.onDoubleTap(e)
        }


    }
}