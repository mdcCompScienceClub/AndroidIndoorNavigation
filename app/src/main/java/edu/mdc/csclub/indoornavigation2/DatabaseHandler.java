package edu.mdc.csclub.indoornavigation2;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nelly on 5/23/17.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHandler.class.getSimpleName();

    private static final int DB_VERSION = 1;
    //The Android's default system path of your application database.
    private static String DB_DIR = "/data/data/edu.mdc.csclub.indoornavigation2/databases/";
    private static String DB_NAME = "indoorLocation.db";

    private SQLiteDatabase db;

    private Context mContext;

    //Constructor
    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     */
    public void createDataBase() {
        boolean dbExist = checkDataBase();
        if (!dbExist) {
            //By calling this method an empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            SQLiteDatabase tempDB = this.getReadableDatabase();
            copyDataBase();
        }
    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(DB_DIR + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            return false;
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    private void copyDataBase() {
        //Open your local db as the input stream
        InputStream inputStream = null;
        try {
            inputStream = mContext.getAssets().open(DB_NAME);
            //Open the empty db as the output stream
            OutputStream outputStream = new FileOutputStream(DB_DIR + DB_NAME, false);


            Log.e(TAG, "******Copying database: " + DB_NAME + " to " + DB_DIR + DB_NAME);

            //transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            //Close the streams
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot open asset = " + e.getMessage());
        }

    }

    public void openDataBase() {
        db = SQLiteDatabase.openDatabase(DB_DIR + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {
        if (db != null)
            db.close();
        super.close();

    }


    /*
    Contains create table statements. Called when database is created.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    /*
    Called when database is upgraded (table structure modified, constraints added, etc.,
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Getting All Measurments
    public List<Measurement> getAllMeasurements() {
        List<Measurement> measurements = new ArrayList<Measurement>();
        // Select All Query
        String selectQuery = "SELECT  * FROM Measurement";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Measurement m = new Measurement();
                m.setX(Integer.parseInt(cursor.getString(0)));
                m.setY(Integer.parseInt(cursor.getString(1)));
                m.setBeacon11RSSI(Integer.parseInt(cursor.getString(2)));
                m.setBeacon12RSSI(Integer.parseInt(cursor.getString(3)));
                m.setBeacon21RSSI(Integer.parseInt(cursor.getString(4)));
                m.setBeacon22RSSI(Integer.parseInt(cursor.getString(5)));
                m.setBeacon31RSSI(Integer.parseInt(cursor.getString(6)));
                m.setBeacon32RSSI(Integer.parseInt(cursor.getString(7)));

                measurements.add(m);
            } while (cursor.moveToNext());
        }

        // return contact list
        return measurements;
    }

    // Getting a cell
    Cell getCell(int X, int Y) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("Cell",                    // table name
                new String[]{"X", "Y", "RoomID"},   // list of which table columns to return
                " X=? AND Y=? ",  // WHERE clause, with ?  as placeholders for parameters
                new String[]{String.valueOf(X), String.valueOf(Y)}, // parameters for the placeholders in the WHERE clause
                null,                               // GROUP BY
                null,                               // HAVING
                null,                               // ORDER BY
                null);                              // ?

        if (cursor != null)
            cursor.moveToFirst();

        //Log.e(TAG, "******Found for X="+ X + " and Y=" + Y + ": "+ cursor.getCount());
        //while(cursor.moveToNext()){
        //    Log.e(TAG, "******" +cursor.getString(0) +"*"+ cursor.getString(1)+"*"+cursor.getString(2)+"*"+cursor.getString(3));
        //}

        Cell cell = null;
        try {
            cell = new Cell(Integer.parseInt(cursor.getString(0)),
                    Integer.parseInt(cursor.getString(1)), Integer.parseInt(cursor.getString(2)));
        } catch (CursorIndexOutOfBoundsException e) {
            Log.e(TAG, "Cell not found");
        }
        return cell;
    }

    // Getting a room
    Room getRoom(int roomId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("Room", new String[]{"_id", "RoomNumber", "RoomPicture", "Description", "OccupiedBy"},
                "_id" + "=?",
                new String[]{String.valueOf(roomId)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Room room = null;
        try {
            room = new Room(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(3), cursor.getString(4), cursor.getString(2));

        } catch (CursorIndexOutOfBoundsException e) {
            Log.e(TAG, "Room not found");
        }
        return room;
    }

}
