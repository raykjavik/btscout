package com.example.btscout

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.Context
import android.telephony.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import android.widget.TextView
import android.widget.Button
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.btscout.database.DatabaseHelper
import com.example.btscout.database.insertCdmaData
import com.example.btscout.database.insertGsmData
import com.example.btscout.database.insertLteData
import com.example.btscout.database.insertWcdmaData

class MainActivity : ComponentActivity() {

    private lateinit var infoCellView: TextView
    private lateinit var infoGPS: TextView
    private lateinit var closeButton: Button
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private var lastLocation: Location? = null
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Ensure this matches your XML file name

        infoCellView = findViewById(R.id.infoCellView)
        closeButton = findViewById(R.id.closeButton)
        infoGPS = findViewById(R.id.infoGPS)

        dbHelper = DatabaseHelper(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE), 1)
            return
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        getCurrentLocation()

        closeButton.setOnClickListener {
            finishAffinity() // This will close the app
        }
    }

    override fun onDestroy() {
        dbHelper.saveDatabaseToExternalStorage()
        dbHelper.close()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentCellInfo(latitude: Double, longitude: Double) {
        val allCellInfo = telephonyManager.allCellInfo
        val connectedCellInfo = allCellInfo.firstOrNull { it.isRegistered }

        if (connectedCellInfo != null) {
            val info = when (connectedCellInfo) {
                is CellInfoGsm -> {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    val timeAdvance = cellSignalStrength.timingAdvance
                    dbHelper.insertGsmData(connectedCellInfo.cellIdentity, cellSignalStrength.dbm, timeAdvance, latitude, longitude)
                    printGsmDetails(connectedCellInfo.cellIdentity, cellSignalStrength, timeAdvance)
                }
                is CellInfoLte -> {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    val timeAdvance = cellSignalStrength.timingAdvance
                    dbHelper.insertLteData(connectedCellInfo.cellIdentity, cellSignalStrength.dbm, timeAdvance, latitude, longitude)
                    printLteDetails(connectedCellInfo.cellIdentity, cellSignalStrength, timeAdvance)
                }
                is CellInfoCdma -> {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertCdmaData(connectedCellInfo.cellIdentity, cellSignalStrength.dbm, latitude, longitude)
                    printCdmaDetails(connectedCellInfo.cellIdentity, cellSignalStrength)
                }
                is CellInfoWcdma -> {
                    val cellSignalStrength = connectedCellInfo.cellSignalStrength
                    dbHelper.insertWcdmaData(connectedCellInfo.cellIdentity, cellSignalStrength.dbm, latitude, longitude)
                    printWcdmaDetails(connectedCellInfo.cellIdentity, cellSignalStrength)
                }
                else -> "Unknown cell type or not supported."
            }

            infoCellView.text = info
        } else {
            infoCellView.text = "No connected cell information available."
        }
    }

    private fun printGsmDetails(identity: CellIdentityGsm, signal: CellSignalStrengthGsm, timeAdvance: Int): String {
        return "GSM Cell Details:\n" +
                "CID: ${identity.cid}\n" +
                "LAC: ${identity.lac}\n" +
                "MCC: ${identity.mcc}\n" +
                "MNC: ${identity.mnc}\n" +
                "ARFCN: ${identity.arfcn}\n" +  // Added ARFCN
                "Signal Strength: ${signal.dbm} dBm\n" +
                "Timing Advance: $timeAdvance\n"
    }

    private fun printLteDetails(identity: CellIdentityLte, signal: CellSignalStrengthLte, timeAdvance: Int): String {
        return "LTE Cell Details:\n" +
                "CI: ${identity.ci}\n" +
                "PCI: ${identity.pci}\n" +
                "TAC: ${identity.tac}\n" +
                "MCC: ${identity.mcc}\n" +
                "MNC: ${identity.mnc}\n" +
                "ARFCN: ${identity.earfcn}\n" +  // Added ARFCN
                "Signal Strength: ${signal.dbm} dBm\n" +
                "Timing Advance: $timeAdvance\n"
    }

    private fun printCdmaDetails(identity: CellIdentityCdma, signal: CellSignalStrengthCdma): String {
        return "CDMA Cell Details:\n" +
                "Network ID: ${identity.networkId}\n" +
                "System ID: ${identity.systemId}\n" +
                "Base Station ID: ${identity.basestationId}\n" +
                "Longitude: ${identity.longitude}\n" +
                "Latitude: ${identity.latitude}\n" +
                "Signal Strength: ${signal.dbm} dBm\n"
    }

    private fun printWcdmaDetails(identity: CellIdentityWcdma, signal: CellSignalStrengthWcdma): String {
        return "WCDMA Cell Details:\n" +
                "CID: ${identity.cid}\n" +
                "LAC: ${identity.lac}\n" +
                "MCC: ${identity.mcc}\n" +
                "MNC: ${identity.mnc}\n" +
                "ARFCN: ${identity.uarfcn}\n" +  // Added ARFCN
                "Signal Strength: ${signal.dbm} dBm\n"
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onGPSUpdate(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
    }

    private fun onGPSUpdate(location: Location) {
        lastLocation?.let { lastLoc ->
            val distance = location.distanceTo(lastLoc)
            if (distance < 20) {
                return
            }
        }

        val locationInfo = "GPS Location:\nLatitude: ${location.latitude}\nLongitude: ${location.longitude}\n\n"
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        infoGPS.text = locationInfo
        infoGPS.append(currentTime)

        getCurrentCellInfo(location.latitude, location.longitude)

        lastLocation = location
    }
}
