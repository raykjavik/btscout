package com.example.btscout.database

import android.content.ContentValues
import android.telephony.*
import android.util.Log

fun DatabaseHelper.insertGsmData(identity: CellIdentityGsm, signalStrength: Int, timingAdvance: Int, latitude: Double, longitude: Double): Long {
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.cid)
                put(DatabaseHelper.COLUMN_LAC, identity.lac)
                put(DatabaseHelper.COLUMN_MCC, identity.mcc)
                put(DatabaseHelper.COLUMN_MNC, identity.mnc)
                put(DatabaseHelper.COLUMN_SIGNAL_STRENGTH, signalStrength)
                put(DatabaseHelper.COLUMN_TIMING_ADVANCE, timingAdvance)
                put(DatabaseHelper.COLUMN_LATITUDE, latitude)
                put(DatabaseHelper.COLUMN_LONGITUDE, longitude)
                put(DatabaseHelper.COLUMN_ARFCN, identity.arfcn)  // Insert ARFCN
            }
            val rowId = db.insert(DatabaseHelper.TABLE_GSM, null, values)
            db.setTransactionSuccessful()  // Marks the transaction as successful
            rowId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting GSM data", e)
            -1L  // Return -1 to indicate failure
        } finally {
            db.endTransaction()  // End the transaction, commit if successful, rollback if not
        }
    }
}

fun DatabaseHelper.insertLteData(identity: CellIdentityLte, signalStrength: Int, timingAdvance: Int, latitude: Double, longitude: Double): Long {
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.ci)
                put(DatabaseHelper.COLUMN_LAC, identity.tac)
                put(DatabaseHelper.COLUMN_MCC, identity.mcc)
                put(DatabaseHelper.COLUMN_MNC, identity.mnc)
                put(DatabaseHelper.COLUMN_SIGNAL_STRENGTH, signalStrength)
                put(DatabaseHelper.COLUMN_TIMING_ADVANCE, timingAdvance)
                put(DatabaseHelper.COLUMN_LATITUDE, latitude)
                put(DatabaseHelper.COLUMN_LONGITUDE, longitude)
                put(DatabaseHelper.COLUMN_ARFCN, identity.earfcn)  // Insert ARFCN
            }
            val rowId = db.insert(DatabaseHelper.TABLE_LTE, null, values)
            db.setTransactionSuccessful()  // Marks the transaction as successful
            rowId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting LTE data", e)
            -1L  // Return -1 to indicate failure
        } finally {
            db.endTransaction()  // End the transaction, commit if successful, rollback if not
        }
    }
}

fun DatabaseHelper.insertCdmaData(identity: CellIdentityCdma, signalStrength: Int, latitude: Double, longitude: Double): Long {
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.basestationId)
                put(DatabaseHelper.COLUMN_LAC, identity.networkId)
                put(DatabaseHelper.COLUMN_MCC, identity.systemId)  // CDMA doesn't have MCC/MNC, so use systemId for MCC
                put(DatabaseHelper.COLUMN_SIGNAL_STRENGTH, signalStrength)
                put(DatabaseHelper.COLUMN_LATITUDE, latitude)
                put(DatabaseHelper.COLUMN_LONGITUDE, longitude)
            }
            val rowId = db.insert(DatabaseHelper.TABLE_CDMA, null, values)
            db.setTransactionSuccessful()  // Marks the transaction as successful
            rowId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting CDMA data", e)
            -1L  // Return -1 to indicate failure
        } finally {
            db.endTransaction()  // End the transaction, commit if successful, rollback if not
        }
    }
}

fun DatabaseHelper.insertWcdmaData(identity: CellIdentityWcdma, signalStrength: Int, latitude: Double, longitude: Double): Long {
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.cid)
                put(DatabaseHelper.COLUMN_LAC, identity.lac)
                put(DatabaseHelper.COLUMN_MCC, identity.mcc)
                put(DatabaseHelper.COLUMN_MNC, identity.mnc)
                put(DatabaseHelper.COLUMN_SIGNAL_STRENGTH, signalStrength)
                put(DatabaseHelper.COLUMN_LATITUDE, latitude)
                put(DatabaseHelper.COLUMN_LONGITUDE, longitude)
                put(DatabaseHelper.COLUMN_ARFCN, identity.uarfcn)  // Insert ARFCN
            }
            val rowId = db.insert(DatabaseHelper.TABLE_WCDMA, null, values)
            db.setTransactionSuccessful()  // Marks the transaction as successful
            rowId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting WCDMA data", e)
            -1L  // Return -1 to indicate failure
        } finally {
            db.endTransaction()  // End the transaction, commit if successful, rollback if not
        }
    }
}
