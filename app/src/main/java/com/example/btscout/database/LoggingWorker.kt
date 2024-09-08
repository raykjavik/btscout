package com.example.btscout.database

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.telephony.*
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.example.btscout.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus.NonExtendable

@Suppress("DEPRECATION")
class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val dbHelper: DatabaseHelper = DatabaseHelper(context)
    private var lastLocation: Location? = null
    private lateinit var telephonyManager: TelephonyManager
    private val notificationId = 1
    private val notificationChannelId = "location_worker_channel"

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        // Start as a foreground service
        setForegroundAsync(createForegroundInfo())

        // Initialize FusedLocationProviderClient for location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        lastLocation = null
        // Run in a blocking loop to fetch and log location every second
        runBlocking {
            while (!isStopped) {
                val location = getLastKnownLocation()
                location?.let { logLocation(it) } // Process the location if available

                // Wait for 1 second before checking the location again
                delay(1000L)
            }
        }

        // Return success when work is done
        return Result.success()
    }

    // Suspend function to fetch the last known location
    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                Tasks.await(fusedLocationClient.lastLocation)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Method to log the location and store it in the database
    @SuppressLint("MissingPermission")
    private fun logLocation(location: Location) {
        if (isStopped) return // Stop processing if the worker has been canceled

        var distance_m = 1000F
        lastLocation?.let {
            distance_m = location.distanceTo(it)
        }

        val allCellInfo = telephonyManager.allCellInfo
        val connectedCellInfo = allCellInfo.firstOrNull { it.isRegistered }

        when (connectedCellInfo) {
            is CellInfoGsm -> {
                val timeAdvance = connectedCellInfo.cellSignalStrength.timingAdvance
                if ((timeAdvance < Integer.MAX_VALUE) && (distance_m > 200F)) {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertGsmData(
                        connectedCellInfo.cellIdentity,
                        cellSignalStrength.dbm,
                        timeAdvance,
                        location.latitude,
                        location.longitude
                    )
                    lastLocation = location
                }
            }
            is CellInfoLte -> {
                val timeAdvance = connectedCellInfo.cellSignalStrength.timingAdvance
                if ((timeAdvance < Integer.MAX_VALUE) && (distance_m > 50F)) {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertLteData(
                        connectedCellInfo.cellIdentity,
                        cellSignalStrength.dbm,
                        timeAdvance,
                        location.latitude,
                        location.longitude
                    )
                    lastLocation = location
                }
            }
        }
    }

    // Create ForegroundInfo to run the worker as a foreground service
    private fun createForegroundInfo(): ForegroundInfo {
        val notification = createNotification()
        return ForegroundInfo(notificationId, notification)
    }

    // Create the notification for the foreground service
    private fun createNotification(): Notification {
        val channelId = notificationChannelId

        // Create notification channel if Android version is Oreo or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Location Worker"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        return NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Tracking Location")
            .setContentText("Your location is being tracked in the background.")
            .setSmallIcon(R.drawable.ic_notification)  // Use an appropriate icon
            .setOngoing(true)
            .build()
    }

    override fun onStopped() {
        super.onStopped()
        // No specific location updates to stop since we're manually polling
    }
}
