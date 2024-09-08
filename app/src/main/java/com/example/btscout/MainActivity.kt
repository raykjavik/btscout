package com.example.btscout

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.work.*
import com.example.btscout.database.DatabaseHelper
import com.example.btscout.database.MyWorker
import com.example.btscout.database.clearGpsAndLteTables
import com.example.btscout.database.getNumberOfGpsAndLteTables
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var infoCellView: TextView
    private lateinit var infoGPS: TextView
    private lateinit var infoLogs: TextView
    private lateinit var logSwitch: Switch
    private lateinit var closeButton: Button
    private lateinit var clearButton: Button
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val updateInterval: Long = 3000 // 3 seconds interval for updates
    private val WORK_TAG = "MyWorkerTag"


    // Register the permission request launcher
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // All permissions granted, proceed with the app logic
                proceedWithAppLogic()
            } else {
                // Permissions denied, show a message and close the app
                Toast.makeText(this, "Permissions denied. The app cannot proceed.", Toast.LENGTH_LONG).show()
                finishAffinity() // Close the app
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check permissions at startup
        if (!PermissionHelper.checkPermissions(this)) {
            requestPermissionsLauncher.launch(PermissionHelper.getRequiredPermissions())
        } else {
            proceedWithAppLogic()
        }
    }

    private fun proceedWithAppLogic() {
        infoCellView = findViewById(R.id.infoCellView)
        closeButton = findViewById(R.id.closeButton)
        infoGPS = findViewById(R.id.infoGPS)
        infoLogs = findViewById(R.id.infoLogs)
        clearButton = findViewById(R.id.clearButton)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        dbHelper = DatabaseHelper(this)

        // Initialize SharedPreferences to save the switch state
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        // Restore the switch state on app launch
        logSwitch = findViewById(R.id.logSwitch)
        logSwitch.isChecked = sharedPreferences.getBoolean("isWorkerRunning", false)
        // Set listener for switch toggling
        logSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startWorker()  // Start the worker when the switch is turned ON
            } else {
                stopWorker()   // Stop the worker when the switch is turned OFF
            }
            // Save the switch state
            saveSwitchState(isChecked)
        }

        closeButton.setOnClickListener { finishAffinity() }

        clearButton.setOnClickListener { showConfirmationDialog() }

        startPeriodicUpdates()
        getCurrentLocation()
    }

    private fun startWorker() {
        // Create the WorkRequest for the Worker
        val workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .addTag(WORK_TAG)  // Add a unique tag to identify the worker
            .build()

        // Enqueue the work using WorkManager
        WorkManager.getInstance(this).enqueueUniqueWork(
            WORK_TAG,
            ExistingWorkPolicy.KEEP,  // Ensure only one worker runs at a time
            workRequest
        )
    }

    private fun stopWorker() {
        // Cancel the worker using its unique tag
        WorkManager.getInstance(this).cancelUniqueWork(WORK_TAG)
    }

    // Save the switch state to SharedPreferences
    private fun saveSwitchState(isChecked: Boolean) {
        sharedPreferences.edit()
            .putBoolean("isWorkerRunning", isChecked)
            .apply()
    }

    private fun startPeriodicUpdates() {
        handler = Handler(mainLooper)
        runnable = object : Runnable {
            override fun run() {
                getCurrentLocation()
                handler.postDelayed(this, updateInterval)
            }
        }
        handler.post(runnable)
    }

    private fun stopPeriodicUpdates() {
        if (::handler.isInitialized && ::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }

    override fun onPause() {
        stopPeriodicUpdates()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        startPeriodicUpdates()
    }

    override fun onDestroy() {
        stopPeriodicUpdates()
        dbHelper.close()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                processNewLocation(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
    }

    private fun processNewLocation(location: Location) {
        val locationInfo = "GPS Location:\nLatitude: ${location.latitude}\nLongitude: ${location.longitude}\n"
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        infoGPS.text = "$locationInfo$currentTime"
        infoLogs.text=dbHelper.getNumberOfGpsAndLteTables()
        displayCellInfo()
    }

    @SuppressLint("MissingPermission")
    private fun displayCellInfo() {
        val allCellInfo = telephonyManager.allCellInfo
        if (allCellInfo.isNullOrEmpty()) {
            infoCellView.text = "No cell information available."
            return
        }

        val connectedCellInfo = allCellInfo.firstOrNull { it.isRegistered }

        if (connectedCellInfo == null) {
            infoCellView.text = "No connected cell information available."
        } else {
            val info = when (connectedCellInfo) {
                is CellInfoGsm -> printGsmDetails(connectedCellInfo.cellIdentity, connectedCellInfo.cellSignalStrength)
                is CellInfoLte -> printLteDetails(connectedCellInfo.cellIdentity, connectedCellInfo.cellSignalStrength)
                else -> "Unknown cell type or not supported."
            }
            infoCellView.text = info
        }
    }

    private fun printGsmDetails(identity: CellIdentityGsm, signal: CellSignalStrengthGsm): String {
        val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mccString ?: "Unknown" else identity.mcc.toString()
        val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mncString ?: "Unknown" else identity.mnc.toString()

        return "GSM Cell Details:\n" +
                "CID: ${identity.cid}\n" +
                "LAC: ${identity.lac}\n" +
                "MCC: $mcc\n" +
                "MNC: $mnc\n" +
                "Signal Strength: ${signal.dbm} dBm\n" +
                "Timing Advance: ${signal.timingAdvance}\n"
    }

    private fun printLteDetails(identity: CellIdentityLte, signal: CellSignalStrengthLte): String {
        val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mccString ?: "Unknown" else identity.mcc.toString()
        val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mncString ?: "Unknown" else identity.mnc.toString()

        return "LTE Cell Details:\n" +
                "CI: ${identity.ci}\n" +
                "PCI: ${identity.pci}\n" +
                "MCC: $mcc\n" +
                "MNC: $mnc\n" +
                "Signal Strength: ${signal.dbm} dBm\n" +
                "Timing Advance: ${signal.timingAdvance}\n"
    }

    private fun clearGsmLteTables() {
        Thread {
            dbHelper.clearGpsAndLteTables()
            runOnUiThread { updateLogStat() }
        }.start()
    }

    private fun updateLogStat() {
        infoLogs.text = dbHelper.getNumberOfGpsAndLteTables()
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Clear Data")
        builder.setMessage("Are you sure you want to delete all GPS and LTE data? This action cannot be undone.")
        builder.setPositiveButton("Yes") { dialog, _ ->
            clearGsmLteTables()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
