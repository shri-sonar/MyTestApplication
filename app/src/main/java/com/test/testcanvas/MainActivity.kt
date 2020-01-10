package com.test.testcanvas

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var mWidth: Int = 0
    private var mHeight: Int = 0

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;
        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mWidth = getScreenWidth() / 2
        mHeight = getScreenHeight() / 4
        clickListeners()
    }

    private fun clickListeners() {

        btnEraser.setOnClickListener {
            if (drawingView?.isEraserActive()!!) {
                drawingView?.deactivateEraser()
                btnEraser.text = "Eraser"
            } else {
                drawingView?.activateEraser()
                btnEraser.text = "Draw"
            }
        }

        btnChooseImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE)
                } else {
                    //permission already granted
                    pickImageFromGallery();
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery()
            }
        }

        btnSave.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getBitmapFromView(drawingView!!, this) {
                    val path = saveImageFile(it)
                    Toast.makeText(this, "Image saved at $path", Toast.LENGTH_LONG).show()
                    Log.e("TAG", "Path $path")
                }
            } else {
                val path = saveImageFile(drawingView!!.getCurrentBitmap())
                Toast.makeText(this, "Image saved at $path", Toast.LENGTH_LONG).show()
                Log.e("TAG", "Path $path")
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getBitmapFromView(view: View, activity: Activity, callback: (Bitmap) -> Unit) {
        activity.window?.let { window ->
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            try {
                PixelCopy.request(
                    window,
                    Rect(
                        locationOfViewInWindow[0],
                        locationOfViewInWindow[1],
                        locationOfViewInWindow[0] + view.width,
                        locationOfViewInWindow[1] + view.height
                    ),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        }
                    },
                    Handler()
                )
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveImageFile(signature: Bitmap): String {
        val outputFile = getRandomPhotoFile()
        try {
            val out = FileOutputStream(outputFile)
            signature.compress(Bitmap.CompressFormat.PNG, 70, out)
            out.close()
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return ""

        }

        return outputFile.absolutePath

    }

    private fun getRandomPhotoFile(): File {

        val root = getExternalFilesDir(null)
        val photoDir = File("${root?.absolutePath}/photo")
        if (!photoDir.exists())
            photoDir.mkdirs()

        val currentTimeInMillis = System.currentTimeMillis()
        return File("${photoDir.absolutePath}/$currentTimeInMillis.jpg")
    }

    private fun pickImageFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, IMAGE_PICK_CODE)
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            if (data != null) {
                var bitmap: Bitmap? = null
                val contentURI = data.data

                bitmap = getBitmapFromUri(contentURI!!);
                if (bitmap != null) {
                    drawingView.addBitmap(bitmap)
                    //mDrawingView.invalidate()
                }
            }
        }
    }

    private fun getBitmapFromUri(data: Uri): Bitmap? {
        var bitmap: Bitmap? = null

        // Starting fetch image from file
        var ins: InputStream? = null
        try {

            ins = contentResolver.openInputStream(data)

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            options.inMutable = true

            // BitmapFactory.decodeFile(path, options);
            BitmapFactory.decodeStream(ins, null, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, mWidth, mHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false

            ins = contentResolver.openInputStream(data)

            bitmap = BitmapFactory.decodeStream(ins, null, options)

            if (bitmap == null) {
                Toast.makeText(baseContext, "Image is not Loaded", Toast.LENGTH_SHORT).show()
                return null
            }

            ins!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        return bitmap
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        return inSampleSize
    }

    private fun getScreenWidth(): Int {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private fun getScreenHeight(): Int {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
