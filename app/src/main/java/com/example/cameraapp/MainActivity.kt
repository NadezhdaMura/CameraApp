package com.example.cameraapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.cameraapp.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import androidx.camera.extensions.BokehImageCaptureExtender
import androidx.camera.extensions.HdrImageCaptureExtender
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraSelector2: CameraSelector
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview() {
        preview = Preview.Builder().build()
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        imageCaptureBtn2.setOnClickListener {
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        }


        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        preview.setSurfaceProvider(binding.previewView.createSurfaceProvider(camera.cameraInfo))
        buildImageCapture()
    }

    private fun buildImageCapture() {
        val builder = ImageCapture.Builder()
        val bokehImageCapture = BokehImageCaptureExtender.create(builder)
        if (bokehImageCapture.isExtensionAvailable(cameraSelector)) {
            bokehImageCapture.enableExtension(cameraSelector)
        }
        val hdrImageCapture = HdrImageCaptureExtender.create(builder)
        if (hdrImageCapture.isExtensionAvailable(cameraSelector)) {
            hdrImageCapture.enableExtension(cameraSelector)
        }
        imageCapture = builder
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(binding.root.display.rotation)
            .build()
        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    }

    fun captureImage() {
        val dialog = AlertDialog.Builder(this)
            .setMessage("Saving picture....")
            .setCancelable(false)
            .show()
        val file = File(
            getExternalFilesDir(null)?.absolutePath,
            System.currentTimeMillis().toString() + ".jpg"
        )
        file.createNewFile()

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, "Image saved!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
    }
}