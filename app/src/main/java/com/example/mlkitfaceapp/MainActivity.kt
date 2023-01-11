package com.example.mlkitfaceapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.squareup.moshi.JsonAdapter
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException


val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .enableTracking()
    .build()



class MainActivity : AppCompatActivity() {
    private lateinit var detector: FaceDetector

    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var previewView: PreviewView? = null
    private var previewUseCase: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisUseCase: ImageAnalysis? = null
    private lateinit var overlay: ViewFinderOverlay
    private lateinit var captureBtn: TextView
    private lateinit var helperText: TextView
    private lateinit var resetBtn: MaterialButton
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overlay = findViewById(R.id.constraint_overlay)
        previewView = findViewById(R.id.preview_view)
        helperText = findViewById(R.id.helper_text)
        captureBtn = findViewById(R.id.camera_capture_button)
        resetBtn = findViewById(R.id.resetBtn)
        email = intent.getStringExtra("email").orEmpty()

        resetBtn.setOnClickListener {
            startCamera()
        }
    }

    override fun onStart() {
        super.onStart()
        initCamera()
    }

    private fun initCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
    }

    private fun closeCamera() {
        detector.close()
    }

    private fun startCamera() {
        detector = FaceDetection.getClient(options)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))[CameraXViewModel::class.java]
            .processCameraProvider
            .observe(
                this,
                Observer { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    bindAllCameraUseCases()
                }
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("Please we need permission to access your camera")
                    .setPositiveButton("Okay") { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", this.packageName, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }.setCancelable(false)
                    .show()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        /*if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }*/
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        /*val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }*/
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }



    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        /*if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =
            try {
                val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
                FaceDetectorProcessor(this, faceDetectorOptions)
            } catch (e: Exception) {
                Log.e("TAG", "Can not create image processor: $selectedModel", e)
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                )
                    .show()
                return
            }*/

        val builder = ImageAnalysis.Builder()
        /*val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }*/
        analysisUseCase = builder.build()

        //needUpdateGraphicOverlayImageSourceInfo = true

        var gestureList = mutableMapOf<Gesture, GestureStatus>()
        val chosenGestures = Gesture.getChosenGestures()
        helperText.text = "Please make this gesture: " +
                "${chosenGestures[0].name.lowercase().replaceFirstChar(Char::uppercase)
                    .replace("_", " ")}"/* +
                "${
                    chosenGestures[1].name.lowercase().replaceFirstChar(Char::uppercase)
                        .replace("_", " ")
                },"+
                "${
                    chosenGestures[2].name.lowercase().replaceFirstChar(Char::uppercase)
                        .replace("_", " ")
                }"*/
        chosenGestures.forEach {
            gestureList[it] = GestureStatus.NOT_DONE
        }

        var trackingID = 0

        val imageCapture = ImageCapture.Builder()
            //.setTargetRotation(view.display.rotation)
            .build()



        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this)
        ) { imageProxy ->
            /*if (needUpdateGraphicOverlayImageSourceInfo) {
                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.width,
                        imageProxy.height,
                        isImageFlipped
                    )
                } else {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        isImageFlipped
                    )
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }*/
            /*try {
                imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                //Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }*/
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        overlay.changeCircleColor(if (faces.isNullOrEmpty().not()) android.R.color.holo_green_light else android.R.color.holo_red_dark)

                        if (faces.any { it.trackingId == trackingID }.not()) { gestureList = gestureList.mapValues { GestureStatus.NOT_DONE }.toMutableMap() }

                        for (face in faces) {
                            if (face.trackingId != null) {
                                trackingID = face.trackingId!!
                                Log.d("detects", trackingID.toString())
                            }
                            /*helperText.text = */
                            val bounds = face.boundingBox
                            val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                            val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                            val rotX = face.headEulerAngleX // Head id up or down

                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                            // nose available):
                            /*val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                            leftEar?.let {
                                val leftEarPos = leftEar.position
                            }*/

                            // If contour detection was enabled:
                            /*val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
                            val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points*/

                            // If classification was enabled:
                            Log.d("reset", "$rotY $rotX $rotZ")

                            if (rotY > 25f && gestureList.containsKey(Gesture.TURN_LEFT)) {
                                //update the required gesture status
                                gestureList[Gesture.TURN_LEFT] = GestureStatus.DONE
                                //update the instruction text
                                (gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                    ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                    .replace("_", " ")
                            }

                            if (rotY < -25f && gestureList.containsKey(Gesture.TURN_RIGHT)) {
                                gestureList[Gesture.TURN_RIGHT] = GestureStatus.DONE

                                (gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                    ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                    .replace("_", " ")
                            }

                            if (rotX > 15f && gestureList.containsKey(Gesture.HEAD_UP)) {
                                gestureList[Gesture.HEAD_UP] = GestureStatus.DONE

                                (gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                    ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                    .replace("_", " ")
                            }

                            if (rotX < -15f && gestureList.containsKey(Gesture.HEAD_DOWN)) {
                                gestureList[Gesture.HEAD_DOWN] = GestureStatus.DONE

                                (gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                    ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                    .replace("_", " ")
                            }

                            if (face.smilingProbability != null) {
                                val smileProb = face.smilingProbability
                                smileProb?.let {
                                    if (it > 0.85f && gestureList.containsKey(Gesture.SMILE)) {
                                        gestureList[Gesture.SMILE] = GestureStatus.DONE

                                        (gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                            ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                            .replace("_", " ")
                                    }
                                }
                            }

                            if (gestureList.all { it.value == GestureStatus.DONE }) {
                                helperText.text = "VERIFIED"
                                helperText.setTextColor(resources.getColor(R.color.light_green))
                                captureBtn.isEnabled = true
                                break
                            } else {
                                //reset or update to latest gesture instruction
                                helperText.text = "${(gestureList.entries.firstOrNull { it.value == GestureStatus.NOT_DONE }?.key?.name
                                    ?:chosenGestures[0].name).lowercase().replaceFirstChar(Char::uppercase)
                                    .replace("_", " ")}"
                                        /*"Please make this gestures: " +
                                        "${chosenGestures[0].name.lowercase().replaceFirstChar(Char::uppercase)
                                            .replace("_", " ")}, " +
                                        "${
                                            chosenGestures[1].name.lowercase().replaceFirstChar(Char::uppercase)
                                                .replace("_", " ")
                                        }, "+
                                        "${
                                            chosenGestures[2].name.lowercase().replaceFirstChar(Char::uppercase)
                                                .replace("_", " ")
                                        }"*/
                                helperText.setTextColor(Color.parseColor("#F4F4F4"))
                                captureBtn.isEnabled = false
                            }
                        }
                    }
                    .addOnFailureListener{}
                    .addOnCompleteListener{
                        imageProxy.close()
                    }
            }
        }
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageCapture,  analysisUseCase, previewUseCase)

        captureBtn.setOnClickListener {
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(externalMediaDirs.first(), "face${System.currentTimeMillis()}.jpg")).build()
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(error: ImageCaptureException) {
                        // insert your code here.
                    }
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        //disable button when no face is detected
                        //change camera to camera 2 or X
                        //change options for detecting liveness(smile, left right)
                        val bm = BitmapFactory.decodeFile(outputFileResults.savedUri?.path)
                        val baos = ByteArrayOutputStream()
                        bm.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            baos
                        ) // bm is the bitmap object

                        val b = baos.toByteArray()
                        val encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
                        cameraProvider?.unbindAll()
                        val pg = ProgressDialog(this@MainActivity)
                        pg.setMessage("Please wait")
                        pg.setCancelable(false)
                        pg.show()


                        Thread {
                            apiService
                                .getFaceMatch(VerifyBody(email, encodedImage))
                                .enqueue(object : Callback<Any> {
                                    override fun onResponse(
                                        call: Call<Any>,
                                        response: Response<Any>
                                    ) {
                                        //Log.d("response", response.body().toString());
                                        if (response.isSuccessful) {
                                            try {
                                                val jsonAdapter: JsonAdapter<Any> = getMoshi().adapter(
                                                    Any::class.java
                                                )
                                                showDialog(
                                                    JSONObject(jsonAdapter.toJson(response.body()))
                                                        .getString("message")
                                                )
                                            } catch (e: JSONException) {
                                                showDialog(e.message)
                                                e.printStackTrace()
                                            }
                                        } else {
                                            try {
                                                showDialog(
                                                    JSONObject(
                                                        response.errorBody()!!.string()
                                                    ).getString("message")
                                                )
                                            } catch (e: IOException) {
                                                showDialog("Something went wrong")
                                                e.printStackTrace()
                                            } catch (e: JSONException) {
                                                showDialog("Something went wrong")
                                                e.printStackTrace()
                                            } catch (e: NullPointerException) {
                                                showDialog("Something went wrong")
                                                e.printStackTrace()
                                            }
                                        }
                                        runOnUiThread { pg.dismiss() }
                                    }

                                    override fun onFailure(call: Call<Any>, t: Throwable) {
                                        showDialog(t.message)
                                        runOnUiThread { pg.dismiss() }
                                    }
                                })
                        }.start()
                    }
                }
            )
        }
    }

    fun showDialog(message: String?) {
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.setMessage(message)
        dialog.setPositiveButton(
            "Ok"
        ) { dialog1, _ -> dialog1.dismiss() }
        dialog.show()
    }
}
