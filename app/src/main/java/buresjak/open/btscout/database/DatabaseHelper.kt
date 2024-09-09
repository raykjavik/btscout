package buresjak.open.btscout.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val context: Context = context.applicationContext

    fun saveDatabaseToExternalStorage() {
        val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
        val appSpecificExternalDir = context.getExternalFilesDir(null)
        val dstFile = File(appSpecificExternalDir, DATABASE_NAME)

        var inputStream: FileInputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = FileInputStream(File(dbPath))
            outputStream = FileOutputStream(dstFile)

            copyFileToStream(inputStream, outputStream)
            println("Database saved to: ${dstFile.absolutePath}, size: ${dstFile.length()} bytes")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to save database: ${e.message}")
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                println("Failed to close streams: ${ioe.message}")
            }
        }
    }

    private fun copyFileToStream(inputStream: FileInputStream, outputStream: FileOutputStream) {
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.flush()
    }

    companion object {
        private const val DATABASE_NAME = "cell_info.db"
        private const val DATABASE_VERSION = 4  // Increment the version for schema changes

        // Table Names
        const val TABLE_GSM = "GSM"
        const val TABLE_LTE = "LTE"

        // Common Columns
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_CID = "cid"
        const val COLUMN_LAC = "lac"
        const val COLUMN_MCC = "mcc"
        const val COLUMN_MNC = "mnc"
        const val COLUMN_SIGNAL_STRENGTH = "signal_strength"
        const val COLUMN_TIMING_ADVANCE = "timing_advance"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_ARFCN = "arfcn"  // Add ARFCN column

        // Create Table Statements
        const val CREATE_TABLE_GSM = (
                "CREATE TABLE $TABLE_GSM (" +
                        "$COLUMN_TIMESTAMP INTEGER PRIMARY KEY," +
                        "$COLUMN_CID TEXT," +
                        "$COLUMN_LAC TEXT," +
                        "$COLUMN_MCC TEXT," +
                        "$COLUMN_MNC TEXT," +
                        "$COLUMN_SIGNAL_STRENGTH INTEGER," +
                        "$COLUMN_TIMING_ADVANCE INTEGER," +
                        "$COLUMN_LATITUDE REAL," +
                        "$COLUMN_LONGITUDE REAL," +
                        "$COLUMN_ARFCN INTEGER" +
                        ")"
                )

        const val CREATE_TABLE_LTE = (
                "CREATE TABLE $TABLE_LTE (" +
                        "$COLUMN_TIMESTAMP INTEGER PRIMARY KEY," +
                        "$COLUMN_CID TEXT," +
                        "$COLUMN_LAC TEXT," +
                        "$COLUMN_MCC TEXT," +
                        "$COLUMN_MNC TEXT," +
                        "$COLUMN_SIGNAL_STRENGTH INTEGER," +
                        "$COLUMN_TIMING_ADVANCE INTEGER," +
                        "$COLUMN_LATITUDE REAL," +
                        "$COLUMN_LONGITUDE REAL," +
                        "$COLUMN_ARFCN INTEGER" +
                        ")"
                )


    }

    override fun onCreate(db: SQLiteDatabase?) {
        println("Creating tables")
        db?.execSQL(CREATE_TABLE_GSM)
        db?.execSQL(CREATE_TABLE_LTE)
        println("Tables created")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GSM")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LTE")
        onCreate(db)
    }

}




