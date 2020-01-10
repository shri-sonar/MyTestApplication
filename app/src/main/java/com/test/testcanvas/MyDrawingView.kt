package com.test.testcanvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Pair
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.abs

class MyDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), View.OnTouchListener {

    private var mCanvas: Canvas? = null

    private var mPath: Path? = null

    private val paths = ArrayList<Pair<Path, Paint>>()

    private var mX: Float = 0.toFloat()
    private var mY: Float = 0.toFloat()

    private val TOUCH_TOLERANCE = 4f

    private var isEraserActive = false

    private var bitmap: Bitmap? = null
    private var imgX = 100f
    private var imgY = 100f
    private var isTouchOnBitmap = false

    private val mPaint =
        Paint().apply {
            isAntiAlias = true
            isDither = true
            color = Color.RED
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 12f
        }

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.WHITE)
        setOnTouchListener(this)
        onCanvasInitialization()
    }

    private fun onCanvasInitialization() {
        mCanvas = Canvas()
        mPath = Path()
        val newPaint = Paint(mPaint)
        paths.add(Pair(mPath!!, newPaint))
    }

    override fun onTouch(arg0: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (bitmap != null) {
                    // check if touch is on bitmap image
                    if (x >= imgX && x < imgX + bitmap?.width!!
                        && y >= imgY && y < imgY + bitmap?.height!!
                    ) {
                        imgX = event.x
                        imgY = event.y
                        isTouchOnBitmap = true
                    } else {
                        isTouchOnBitmap = false
                        actionDown(x, y)
                    }
                } else {
                    isTouchOnBitmap = false
                    actionDown(x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTouchOnBitmap) {
                    imgX = event.x
                    imgY = event.y
                } else {
                    actionMove(x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (!isTouchOnBitmap) {
                    actionUp()
                }
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, imgX, imgY, null)
        }

        for (p in paths) {
            canvas.drawPath(p.first, p.second)
        }
    }

    private fun actionDown(x: Float, y: Float) {
        if (isEraserActive) {
            mPaint.color = Color.WHITE
            mPaint.strokeWidth = 12f
            val newPaint = Paint(mPaint) // Clones the mPaint object
            paths.add(Pair<Path, Paint>(mPath, newPaint))
        } else {
            mPaint.color = Color.RED
            mPaint.strokeWidth = 12f
            val newPaint = Paint(mPaint) // Clones the mPaint object
            paths.add(Pair<Path, Paint>(mPath, newPaint))
        }

        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun actionMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }

    private fun actionUp() {
        mPath!!.lineTo(mX, mY)

        // commit the path to our offscreen
        mCanvas!!.drawPath(mPath!!, mPaint)

        // kill this so we don't double draw
        mPath = Path()
        val newPaint = Paint(mPaint) // Clones the mPaint object
        paths.add(Pair(mPath!!, newPaint))
    }

    fun activateEraser() {
        isEraserActive = true
    }

    fun deactivateEraser() {
        isEraserActive = false
    }

    fun isEraserActive(): Boolean {
        return isEraserActive
    }

    fun getCurrentBitmap(): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun addBitmap(selectedImage: Bitmap) {
        bitmap = selectedImage
    }
}