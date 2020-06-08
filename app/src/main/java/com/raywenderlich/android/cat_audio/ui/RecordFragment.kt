/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.cat_audio.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import com.raywenderlich.android.cat_audio.service.MediaCaptureService
import com.raywenderlich.android.cataudio.R
import kotlinx.android.synthetic.main.fragment_record.*


class RecordFragment : Fragment(R.layout.fragment_record) {
  private lateinit var mediaProjectionManager: MediaProjectionManager

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    button_start_recording.setOnClickListener {
      startCapturing()
    }

    button_stop_recording.setOnClickListener {
      stopCapturing()
    }
  }

  private fun setButtonsEnabled(isCapturingAudio: Boolean) {
    button_start_recording.isEnabled = !isCapturingAudio
    button_stop_recording.isEnabled = isCapturingAudio
  }

  private fun startCapturing() {
    if (!isRecordAudioPermissionGranted()) {
      requestRecordAudioPermission()
    } else {
      startMediaProjectionRequest()
    }
  }

  private fun stopCapturing() {
    setButtonsEnabled(isCapturingAudio = false)

    startForegroundService(requireContext(), Intent(requireContext(), MediaCaptureService::class.java).apply {
      action = MediaCaptureService.ACTION_STOP
    })
  }

  private fun isRecordAudioPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun requestRecordAudioPermission() {
    ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(Manifest.permission.RECORD_AUDIO),
        RECORD_AUDIO_PERMISSION_REQUEST_CODE
    )
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
      if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(
            requireContext(),
            "Permissions to capture audio granted. Click the button once again.",
            Toast.LENGTH_SHORT
        ).show()
      } else {
        Toast.makeText(
            requireContext(), "Permissions to capture audio denied.",
            Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  /**
   * Before a capture session can be started, the capturing app must
   * call MediaProjectionManager.createScreenCaptureIntent().
   * This will display a dialog to the user, who must tap "Start now" in order for a
   * capturing session to be started. This will allow both video and audio to be captured.
   */
  private fun startMediaProjectionRequest() {
    // use applicationContext to avoid memory leak on Android 10.
    // see: https://partnerissuetracker.corp.google.com/issues/139732252
    mediaProjectionManager =
        requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    startActivityForResult(
        mediaProjectionManager.createScreenCaptureIntent(),
        MEDIA_PROJECTION_REQUEST_CODE
    )
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        Toast.makeText(
            requireContext(),
            "MediaProjection permission obtained. Foreground service will be started to capture audio.",
            Toast.LENGTH_SHORT
        ).show()

        val audioCaptureIntent = Intent(requireContext(), MediaCaptureService::class.java).apply {
          action = MediaCaptureService.ACTION_START
          putExtra(MediaCaptureService.EXTRA_RESULT_DATA, data!!)
        }
        startForegroundService(requireContext(), audioCaptureIntent)

        setButtonsEnabled(isCapturingAudio = true)
      } else {
        Toast.makeText(
            requireContext(), "Request to obtain MediaProjection denied.",
            Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  companion object {
    private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42
    private const val MEDIA_PROJECTION_REQUEST_CODE = 13
  }
}
