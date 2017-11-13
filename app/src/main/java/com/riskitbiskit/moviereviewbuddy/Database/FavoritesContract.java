package com.riskitbiskit.moviereviewbuddy.Database;

import android.net.Uri;
import android.provider.BaseColumns;

public final class FavoritesContract {

    private FavoritesContract() {}

    public static final String CONTENT_AUTHORITY = "com.riskitbiskit.moviereviewbuddy";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FAVE = "favorite";

    public static  class FavoritesEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FAVE);

        public static final String TABLE_NAME = PATH_FAVE;

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_MOVIE_TITLE = "title";
        public static final String COLUMN_MOVIE_OVERVIEW = "overview";
        public static final String COLUMN_MOVIE_RATING = "rating";
        public static final String COLUMN_MOVIE_POSTER = "posterPath";
        public static final String COLUMN_MOVIE_RELEASE_DATE = "releaseDate";
        public static final String COLUMN_MOVIE_API_ID = "apiNumber";
    }

}
