/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mlkitfaceapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LiveData
import com.example.mlkitfaceapp.CameraXViewModel
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutionException
import kotlin.random.Random

/** View model for interacting with CameraX.  */
class CameraXViewModel(application: Application) : AndroidViewModel(application) {
    private var cameraProviderLiveData: MutableLiveData<ProcessCameraProvider>? = null

    // Handle any errors (including cancellation) here.
    val processCameraProvider: LiveData<ProcessCameraProvider> get() {
            if (cameraProviderLiveData == null) {
                cameraProviderLiveData = MutableLiveData()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
                cameraProviderFuture.addListener(
                    {
                        try {
                            cameraProviderLiveData!!.setValue(cameraProviderFuture.get())
                        } catch (e: ExecutionException) {
                            // Handle any errors (including cancellation) here.
                            Log.e(TAG, "Unhandled exception", e)
                        } catch (e: InterruptedException) {
                            Log.e(TAG, "Unhandled exception", e)
                        }
                    },
                    ContextCompat.getMainExecutor(getApplication())
                )
            }
            return cameraProviderLiveData!!
    }

    companion object {
        private const val TAG = "CameraXViewModel"
    }
}


enum class Gesture(val index: Int) {
    SMILE(0), TURN_LEFT(1), TURN_RIGHT(2), HEAD_UP(3), HEAD_DOWN(4);

    companion object {
        fun getChosenGestures(): List<Gesture> {
            return values().let {
                it.shuffle()
                it.take(3)
            }
        }
    }
}

enum class GestureStatus {
    DONE, NOT_DONE
}