package com.konar.partyflashlight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.konar.partyflashlight.databinding.ActivityMainBinding
import java.nio.file.Files.delete
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var timer: Timer? = null
    private var cameraManager: CameraManager? = null
    private var getCameraId: String? = null

    val requestPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this@MainActivity, "Permission denied!!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        ) {
            showRationaleDialog(
                "Camera permission required",
                "This app requires camera permission to use flashlight"
            )
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            getCameraId = cameraManager!!.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        }

        binding.btnFast.setOnClickListener {
            flash(150)
        }

        binding.btnMed.setOnClickListener {
            flash(200)
        }

        binding.btnSlow.setOnClickListener {
            flash(250)
        }

        binding.btnStop.setOnClickListener {
            cameraManager!!.setTorchMode(getCameraId!!, false)
            if(timer != null) {
                timer!!.cancel()
                timer!!.purge()
                timer = null
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun flash(time: Long) {

        timer?.cancel()
        timer?.purge()
        timer = null
        timer = Timer()
        timer!!.scheduleAtFixedRate(0, 2*time) {
            try {
                cameraManager!!.setTorchMode(getCameraId!!, true)

                try {
                    Thread.sleep(time)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                cameraManager!!.setTorchMode(getCameraId!!, false)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraManager!!.setTorchMode(getCameraId!!, false)

        timer?.cancel()
        timer?.purge()
        timer = null
    }
}