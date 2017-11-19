package com.riskitbiskit.moviereviewbuddy.Database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.FavoritesEntry;

public class FavoritesContentProvider extends ContentProvider {

    //Testing
    public static final String LOG_TAG = FavoritesContentProvider.class.getSimpleName();

    //Constants
    public static final int FAVES = 100;
    public static final int FAVES_WITH_ID = 101;

    //Variables
    private FavoritesDBHelper mDBHelper;

    //Matcher
    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(FavoritesContract.CONTENT_AUTHORITY, FavoritesContract.PATH_FAVE, FAVES);
        sUriMatcher.addURI(FavoritesContract.CONTENT_AUTHORITY, FavoritesContract.PATH_FAVE + "/#", FAVES_WITH_ID);
    }

    @Override
    public boolean onCreate() {
        //create new database
        mDBHelper = new FavoritesDBHelper(getContext());

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //get a reference to a readable database
        SQLiteDatabase database = mDBHelper.getReadableDatabase();

        //declare return cursor
        Cursor returnCursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FAVES:
                returnCursor = database.query(FavoritesEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return returnCursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FAVES:
                //get a writable version of the database
                SQLiteDatabase database = mDBHelper.getWritableDatabase();

                long id = database.insert(FavoritesEntry.TABLE_NAME, null, contentValues);

                if (id == -1) {
                    Log.e(LOG_TAG, "Failed to insert row for " + uri);
                    return null;
                }

                return ContentUris.withAppendedId(uri, id);

            default:
                throw  new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        //get reference to writable version of the database
        SQLiteDatabase database = mDBHelper.getWritableDatabase();

        int rowsDeleted;

        int match = sUriMatcher.match(uri);
        switch (match) {

            case FAVES_WITH_ID:

                selection = FavoritesEntry._ID + "=?";

                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};

                rowsDeleted = database.delete(FavoritesEntry.TABLE_NAME, selection, selectionArgs);

                return rowsDeleted;

            default:
                throw new IllegalArgumentException("Cannot delete from favorites");
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }


    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
