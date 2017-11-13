package com.riskitbiskit.moviereviewbuddy.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.FavoritesEntry;

public class FavoritesDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "favorites.db";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + FavoritesEntry.TABLE_NAME + " (" +
            FavoritesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FavoritesEntry.COLUMN_MOVIE_TITLE + " TEXT NOT NULL, " +
            FavoritesEntry.COLUMN_MOVIE_OVERVIEW + " TEXT NOT NULL, " +
            FavoritesEntry.COLUMN_MOVIE_RATING + " REAL NOT NULL, " +
            FavoritesEntry.COLUMN_MOVIE_POSTER + " TEXT NOT NULL, " +
            FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE + " TEXT NOT NULL, " +
            FavoritesEntry.COLUMN_MOVIE_API_ID + " INTEGER NOT NULL" +
            ");";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE " + FavoritesEntry.TABLE_NAME;

    public FavoritesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}
