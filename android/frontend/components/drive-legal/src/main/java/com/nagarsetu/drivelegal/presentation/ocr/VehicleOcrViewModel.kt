package com.nagarsetu.drivelegal.presentation.ocr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VehicleOcrViewModel @Inject constructor() : ViewModel() {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val _ocrResult = MutableStateFlow<String?>(null)
    val ocrResult = _ocrResult.asStateFlow()

    private val platePattern = Regex("[A-Z]{2}\\s?[0-9]{2}\\s?[A-Z]{1,2}\\s?[0-9]{4}")

    fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val match = platePattern.find(visionText.text.uppercase())
                _ocrResult.value = match?.value ?: "No valid plate detected"
            }
            .addOnFailureListener {
                _ocrResult.value = "Error: ${it.message}"
            }
    }

    fun mockScan() {
        _ocrResult.value = "MP 04 AB 1234"
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}
