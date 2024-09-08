package com.example.btscout.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.telephony.*
import android.util.Log




fun DatabaseHelper.insertGsmData(identity: CellIdentityGsm, signalStrength: Int, timingAdvance: Int, latitude: Double, longitude: Double): Long {
    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mccString ?: "Unknown" else identity.mcc.toString()
    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mncString ?: "Unknown" else identity.mnc.toString()
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.cid)
                put(DatabaseHelper.COLUMN_LAC, identity.lac)
                put(DatabaseHelper.COLUMN_MCC, mcc)
                put(DatabaseHelper.COLUMN_MNC, mnc)
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
    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mccString ?: "Unknown" else identity.mcc.toString()
    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mncString ?: "Unknown" else identity.mnc.toString()
    return this.writableDatabase.use { db ->
        try {
            db.beginTransaction()
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())  // Using current time as the timestamp
                put(DatabaseHelper.COLUMN_CID, identity.ci)
                put(DatabaseHelper.COLUMN_LAC, identity.tac)
                put(DatabaseHelper.COLUMN_MCC, mcc)
                put(DatabaseHelper.COLUMN_MNC, mnc)
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

// Function to clear both GPS and LTE tables
fun DatabaseHelper.clearGpsAndLteTables() {
    writableDatabase.use { db ->
        try {
            db.beginTransaction()

            // Drop GSM table if it exists
            db.execSQL("DROP TABLE IF EXISTS ${DatabaseHelper.TABLE_GSM}")

            // Drop LTE table if it exists
            db.execSQL("DROP TABLE IF EXISTS ${DatabaseHelper.TABLE_LTE}")

            // Recreate the GSM table
            db.execSQL(DatabaseHelper.CREATE_TABLE_GSM)

            // Recreate the LTE table
            db.execSQL(DatabaseHelper.CREATE_TABLE_LTE)

            db.setTransactionSuccessful()  // Commit the transaction if everything is successful
            Log.i("DatabaseHelper", "Successfully dropped and recreated GSM and LTE tables.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error dropping and recreating GSM and LTE tables", e)
        } finally {
            db.endTransaction()  // End the transaction, rollback if there was an error
        }
    }
}

// Function to get the number of GPS and LTE tables
fun DatabaseHelper.getNumberOfGpsAndLteTables(): String {
    val db = this.readableDatabase // Get readable database

    val gpsCount = getTableRowCount(db, "GSM")
    val lteCount = getTableRowCount(db, "LTE")

    // Close the database connection when done
    db.close()

    // Return the formatted result as a two-line string
    return """
        points:
        GPS: $gpsCount
        LTE: $lteCount
    """.trimIndent()
}

// Helper function to count the number of tables with a specific type
fun DatabaseHelper.getTableRowCount(db: SQLiteDatabase, tableName: String): Int {
    // Directly query the table for the number of rows
    val query = "SELECT COUNT(*) FROM $tableName"
    db.rawQuery(query, null).use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getInt(0)  // Return the number of rows in the table
        }
    }
    return 0  // If the query fails for some reason, return 0
}