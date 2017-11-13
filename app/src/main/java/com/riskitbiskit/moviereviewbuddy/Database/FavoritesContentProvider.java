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
    public static final String LOG_TAG = FavoritesContentProvider.class.getSimpleName();

    private FavoritesDBHelper mDBHelper;

    public static final int FAVES = 100;
    public static final int FAVES_WITH_ID = 101;

    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(FavoritesContract.CONTENT_AUTHORITY, FavoritesContract.PATH_FAVE, FAVES);
        sUriMatcher.addURI(FavoritesContract.CONTENT_AUTHORITY, FavoritesContract.PATH_FAVE + "/#", FAVES_WITH_ID);
    }

    @Override
    public boolean onCreate() {
        mDBHelper = new FavoritesDBHelper(getContext());

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDBHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FAVES:
                cursor = database.query(FavoritesEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FAVES:
                SQLiteDatabase database = mDBHelper.getWritableDatabase();

                long id = database.insert(FavoritesEntry.TABLE_NAME, null, contentValues);

                if (id == -1) {
                    Log.e(LOG_TAG, "Failed to insert row for " + uri);
                    return null;
                }

//                getContext().getContentResolver().notifyChange(uri, null);

                return ContentUris.withAppendedId(uri, id);
            default:
                throw  new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase database = mDBHelper.getWritableDatabase();

        int rowsDeleted;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FAVES_WITH_ID:
                selection = FavoritesEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};

                rowsDeleted = database.delete(FavoritesEntry.TABLE_NAME, selection, selectionArgs);

//                if (rowsDeleted != 0) {
//                    getContext().getContentResolver().notifyChange(uri, null);
//                }

                return rowsDeleted;
            default:
                throw new IllegalArgumentException("Cannot delete from favorites");
        }

    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
