package buresjak.open.btscout.database
import java.util.concurrent.TimeUnit
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.telephony.*
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import buresjak.open.btscout.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

@Suppress("DEPRECATION")
class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val dbHelper: DatabaseHelper = DatabaseHelper(context)
    private var lastLocation: Location? = null
    private lateinit var telephonyManager: TelephonyManager
    private val notificationId = 1
    private val notificationChannelId = "location_worker_channel"

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        // Initialize FusedLocationProviderClient for location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        // Start as a foreground service
        setForegroundAsync(createForegroundInfo())

        telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        lastLocation = null

        // Run in a blocking loop to fetch and log location every second
        runBlocking {
            while (!isStopped) {
                // Get the current location with high accuracy using Tasks.await
                val location = getCurrentLocation()
                location?.let { logLocation(it) } // Process the location if available

                // Wait for 1 second before checking the location again
                delay(1000L)
            }
        }

        // Return success when work is done
        return Result.success()
    }

    // Suspend function to fetch the current location with high accuracy
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                val locationTask = fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                if (locationTask != null) {
                    Tasks.await(locationTask, 5, TimeUnit.SECONDS) // Timeout after 5 seconds
                } else {
                    null
                }
            } catch (e: Exception) {
                null // Return null in case of any exception or timeout
            }
        }
    }

    // Method to log the location and store it in the database
    @SuppressLint("MissingPermission")
    private fun logLocation(location: Location) {
        if (isStopped) return // Stop processing if the worker has been canceled

        var distanceMeters = 1000F
        lastLocation?.let {
            distanceMeters = location.distanceTo(it)
        }

        val allCellInfo = telephonyManager.allCellInfo
        val connectedCellInfo = allCellInfo.firstOrNull { it.isRegistered }

        when (connectedCellInfo) {
            is CellInfoGsm -> {
                val timeAdvance = connectedCellInfo.cellSignalStrength.timingAdvance
                if ((timeAdvance in 0 until Integer.MAX_VALUE) && (distanceMeters > 200F)) {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertGsmData(
                        connectedCellInfo.cellIdentity,
                        cellSignalStrength.dbm,
                        timeAdvance,
                        location.latitude,
                        location.longitude
                    )
                    lastLocation = location
                } else {
                    // Force some network interaction to try and update Timing Advance
                    forceNetworkInteraction()
                }
            }
            is CellInfoLte -> {
                val timeAdvance = connectedCellInfo.cellSignalStrength.timingAdvance
                if ((timeAdvance in 0 until Integer.MAX_VALUE) && (distanceMeters > 50F)) {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertLteData(
                        connectedCellInfo.cellIdentity,
                        cellSignalStrength.dbm,
                        timeAdvance,
                        location.latitude,
                        location.longitude
                    )
                    lastLocation = location
                } else {
                    // Force some network interaction to try and update Timing Advance
                    forceNetworkInteraction()
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

    // Method to force network interaction by performing a minimal network operation
    private fun forceNetworkInteraction() {
        val url = URL("https://example.com/ping")
        Thread {
            try {
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 500 // Reduced timeout to 500 milliseconds
                urlConnection.inputStream
                urlConnection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace() // Handle any exception here, if necessary
            }
        }.start()
    }

    override fun onStopped() {
        super.onStopped()
        // No need to remove location updates since we're using getCurrentLocation()
    }
}
