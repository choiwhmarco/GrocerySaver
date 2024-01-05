package cse110.grocerysaver.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateUtils;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cse110.grocerysaver.database.DatabaseContract.FridgeItem;
import cse110.grocerysaver.database.DatabaseContract.Favorite;
import cse110.grocerysaver.database.DatabaseContract.InventoryItem;

/**
 *
 * This class is for handling database creation and upgrades. It can also be used to interact with
 * the database.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GrocerySaver.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_TABLE_FRIDGE_ITEM =
            "CREATE TABLE " + FridgeItem.TABLE + " (" +
                    FridgeItem._ID + " INTEGER PRIMARY KEY," +
                    FridgeItem.COLUMN_NAME + " TEXT," +
                    FridgeItem.COLUMN_DATE_ADDED + " LONG," +
                    FridgeItem.COLUMN_EXPIRATION_DATE + " LONG," +
                    FridgeItem.COLUMN_NOTES + " TEXT)";

    private static final String SQL_CREATE_TABLE_FAVORITE =
            "CREATE TABLE " + Favorite.TABLE + " (" +
                    Favorite._ID + " INTEGER PRIMARY KEY," +
                    Favorite.COLUMN_NAME + " TEXT," +
                    Favorite.COLUMN_SHELF_LIFE + " LONG," +
                    Favorite.COLUMN_NOTES + " TEXT)";

    private static final String SQL_CREATE_TABLE_INVENTORY_ITEM =
            "CREATE TABLE " + InventoryItem.TABLE + " (" +
                    InventoryItem._ID + " INTEGER PRIMARY KEY," +
                    InventoryItem.COLUMN_NAME + " TEXT UNIQUE," +
                    InventoryItem.COLUMN_SHELF_LIFE + " LONG)";

    private static final String SQL_DROP_TABLE_FRIDGE_ITEM =
            "DROP TABLE IF EXISTS " + FridgeItem.TABLE;

    private static final String SQL_DROP_TABLE_FAVORITE =
            "DROP TABLE IF EXISTS " + Favorite.TABLE;

    private static final String SQL_DROP_TABLE_INVENTORY_ITEM =
            "DROP TABLE IF EXISTS " + InventoryItem.TABLE;

    private static final String INV_FILENAME = "inventoryData.json";

    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_FRIDGE_ITEM);
        db.execSQL(SQL_CREATE_TABLE_FAVORITE);
        db.execSQL(SQL_CREATE_TABLE_INVENTORY_ITEM);
        try {
            InputStream in =  context.getAssets().open(INV_FILENAME);
            readInventory (db, in);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readInventory(SQLiteDatabase db, InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {

            // first array records first object that begins with certain letter
            reader.beginObject();
            reader.nextName();
            reader.skipValue();

            // second array contains actual data
            // format: {name: string, fullname: string, expiry: array}
            reader.nextName();
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                String foodName = "";
                while (reader.hasNext()) {

                    String name = reader.nextName();

                    if ("name".equals(name)) {
                        String rawName = reader.nextString();
                        foodName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);
                    }
                    else if ("expiry".equals(name)) {
                        readExpiry(db, reader, foodName);
                    }
                    else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            reader.endArray();
            reader.endObject();
        }
        finally {
            reader.close();
        }
    }

    private void readExpiry(SQLiteDatabase db, JsonReader reader, String foodName) throws IOException {


        reader.beginArray();

        while (reader.hasNext()) {
            String invName = foodName;
            String invNameTyped = "";

            // format: {type: string, refrigerator: days, pantry: days, freezer: days}
            reader.beginObject();
            while (reader.hasNext()) {
                String expName = reader.nextName();

                if ("type".equals(expName)) {
                    String type = reader.nextString();

                    if ("refContainer".equals(type)) {
                        invName += " (container) ";
                    }
                    else if (!(type.equals("default")) && !(type.equals("frozen"))) {
                        invName += " (" + type + ") ";
                    }
                    invNameTyped = invName;
                }
                else {
                    long expDays = reader.nextLong();
                    long shelfLife = expDays * DateUtils.DAY_IN_MILLIS;
                    if (expDays > 0) {
                        if ("refrigerator".equals(expName)) {
                            invName += " (refrigerator) ";
                        }
                        else if ("pantry".equals(expName)) {
                            invName += " (pantry) ";
                        }
                        else if ("freezer".equals(expName)) {
                            invName += " (freezer) ";
                        }

                        // insert one item per type/storage combination (if exists)
                        ContentValues values = new ContentValues();
                        values.put(InventoryItem.COLUMN_NAME, invName);
                        values.put(InventoryItem.COLUMN_SHELF_LIFE, shelfLife);

                        /*
                         * Duplicates possible d/t imperfect scraping and time ranges
                         * e.g. {type: default, refrigerator: -1, pantry: 30, freezer: 60},
                         *      {type: default, refrigerator: 10, pantry: 60, freezer: -1}
                         *
                         * so just insert latest
                         * (to insert earliest, use CONFLICT_IGNORE)
                         */
                        db.insertWithOnConflict(InventoryItem.TABLE, null, values,
                                SQLiteDatabase.CONFLICT_REPLACE);

                        invName = invNameTyped;
                    }
                }

            }
            reader.endObject();
        }
        reader.endArray();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL(SQL_DROP_TABLE_FRIDGE_ITEM);
        db.execSQL(SQL_DROP_TABLE_FAVORITE);
        db.execSQL(SQL_DROP_TABLE_INVENTORY_ITEM);
        onCreate(db);
    }
}
