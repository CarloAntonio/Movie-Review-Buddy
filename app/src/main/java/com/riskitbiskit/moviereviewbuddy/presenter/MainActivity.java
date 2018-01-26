package com.riskitbiskit.moviereviewbuddy.presenter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.FavoritesEntry;
import com.riskitbiskit.moviereviewbuddy.adapters.MovieArrayAdapter;
import com.riskitbiskit.moviereviewbuddy.R;
import com.riskitbiskit.moviereviewbuddy.model.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    //Testing
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    //Constants
    public static final String API_KEY = "9248410cd68fafd7d26df8b01e1057b0";
    public static final String ROOT_URL = "https://api.themoviedb.org/3";
    public static final String TOP_RATED_PATH = "/movie/top_rated";
    public static final String MOST_POPULAR_PATH = "/movie/popular";
    //public static final String NOW_PLAYING_PATH = "/movie/now_playing"; //Future Path?
    public static final String ON_SAVE_INSTANCE_STATE_KEY = "key";
    public static final String FAVE_LIST_BOOLEAN = "faveBoolean";
    public static final int MOVIE_LOADER = 0;
    private static final int NETWORK_ERROR = 1;
    private static final int REQUEST_ERROR = 2;
    public static final int EMPTY_FAVORITES = 3;
    private static final String SAVED_EMPTY_VIEW_VALUE = "saved_empty_view_value";

    //Views
    @BindView(R.id.no_internet_view)
    RelativeLayout noInternetView;
    @BindView(R.id.content_grid_view)
    GridView moviesGridView;
    @BindView(R.id.no_internet_tv)
    TextView noInternetTV;
    @BindView(R.id.no_internet_iv)
    ImageView noInternetIV;

    //Fields
    private MovieArrayAdapter mMovieArrayAdapter;
    private boolean isFavoritesList = false;
    private int currentEmptyViewValue = 1;
    OkHttpClient mOkHttpClient;
    List<Movie> movies;

    //save relevant data
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        //package relevant info
        outState.putParcelableArrayList(ON_SAVE_INSTANCE_STATE_KEY, (ArrayList<? extends Parcelable>) movies);
        outState.putBoolean(FAVE_LIST_BOOLEAN, isFavoritesList);
        outState.putInt(SAVED_EMPTY_VIEW_VALUE, currentEmptyViewValue);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //Instantiate new array list
        movies = new ArrayList<>();

        //Setup adapter
        mMovieArrayAdapter = new MovieArrayAdapter(this, R.layout.movie_poster_item, new ArrayList<Movie>());

        if (savedInstanceState != null) {
            extractInstanceState(savedInstanceState);
        } else {
            // if/else statement allows favorite page to be viewed outside of a network
            if (isFavoritesList) {
                noInternetView.setVisibility(View.INVISIBLE);
            } else {
                makeNetworkRequest(MOST_POPULAR_PATH);
            }
        }

        moviesGridView.setAdapter(mMovieArrayAdapter);

        moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Get chosen movie
                Movie chosenMovie = mMovieArrayAdapter.getItem(i);

                //Create intent and pass in chosen movie
                Intent intent = new Intent(getBaseContext(), DetailsActivity.class);
                intent.putExtra(Movie.MOVIE, chosenMovie);

                //Start activity for result
                startActivity(intent);
            }
        });
    }

    private void makeNetworkRequest(String path) {
        if (getActiveNetworkInfo() != null && getActiveNetworkInfo().isConnected()) {
            makeNetworkCall(path);
        } else {
            showEmptyView(NETWORK_ERROR);
        }
    }

    private void extractInstanceState(Bundle savedInstanceState) {
        //pull boolean value for deciding for view wants to see favorites list
        isFavoritesList = savedInstanceState.getBoolean(FAVE_LIST_BOOLEAN);
        //pull list from saved instance state
        movies = savedInstanceState.getParcelableArrayList(ON_SAVE_INSTANCE_STATE_KEY);
        //clean and add new movies to adapter
        if (movies != null && movies.size() == 0) {
            showEmptyView(savedInstanceState.getInt(SAVED_EMPTY_VIEW_VALUE));
        }
        refreshAdapter();
    }

    private void refreshAdapter() {
        //clean up old data from array adapter
        mMovieArrayAdapter.clear();
        //add saved data to array adapter
        mMovieArrayAdapter.addAll(movies);
    }

    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    private void makeNetworkCall(String requestedPath) {
        mOkHttpClient = new OkHttpClient();

        Uri baseUri = Uri.parse(ROOT_URL + requestedPath);
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter("api_key", API_KEY);
        builder.appendQueryParameter("language", "en-US");

        Request request = new Request.Builder()
                .url(builder.toString())
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showEmptyView(REQUEST_ERROR);
                Log.e(LOG_TAG, "Error requesting data from server");
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processResponse(response);
                    }
                });
            }
        });
    }

    private void processResponse(Response response) {
        try {
            //parse through json response
            String jsonResponse = response.body().string();
            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONArray resultsArray = rootObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentMovieObject = resultsArray.getJSONObject(i);

                //extract data from current movie
                String movieTitle = currentMovieObject.getString("original_title");
                String movieOverview = currentMovieObject.getString("overview");
                double movieRating = currentMovieObject.getDouble("vote_average");
                String moviePosterPath = currentMovieObject.getString("poster_path");
                String movieReleaseDate = currentMovieObject.getString("release_date");
                long movieId = currentMovieObject.getLong("id");

                //add to list of movies
                movies.add(new Movie(movieTitle, movieOverview, movieRating, moviePosterPath, movieReleaseDate, movieId));
            }

            //remove empty view
            noInternetView.setVisibility(View.INVISIBLE);
            moviesGridView.setVisibility(View.VISIBLE);

            //clean and add new movies to adapter
            refreshAdapter();

        } catch (JSONException e) {
            showEmptyView(REQUEST_ERROR);
            Log.e(LOG_TAG, "Problem parsing the movie JSON results", e);
        } catch (IOException e) {
            showEmptyView(REQUEST_ERROR);
            Log.e(LOG_TAG, "Problem with I/O", e);
        }
    }

    private void showEmptyView(int error_type) {
        switch (error_type) {
            case NETWORK_ERROR:
                currentEmptyViewValue = error_type;
                noInternetTV.setText(getResources().getText(R.string.no_internet_connectivity));
                noInternetIV.setImageResource(R.drawable.no_internet);
                noInternetView.setVisibility(View.VISIBLE);
                moviesGridView.setVisibility(View.INVISIBLE);
                break;
            case REQUEST_ERROR:
                currentEmptyViewValue = error_type;
                noInternetTV.setText(R.string.error_requesting_data);
                noInternetIV.setImageResource(R.drawable.mark);
                noInternetView.setVisibility(View.VISIBLE);
                moviesGridView.setVisibility(View.INVISIBLE);
                break;
            case EMPTY_FAVORITES:
                currentEmptyViewValue = error_type;
                noInternetTV.setText(R.string.no_movies_added);
                noInternetIV.setImageResource(R.drawable.mark);
                noInternetView.setVisibility(View.VISIBLE);
                moviesGridView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // identify specific menu item click
        int id = item.getItemId();

        // handle action bar item clicks
        if (id == R.id.action_popular) {
            //clear movies list
            movies.clear();
            makeNetworkRequest(MOST_POPULAR_PATH);
            isFavoritesList = false;
            return true;
        } else if (id == R.id.action_top_rated) {
            //clear movies list
            movies.clear();
            makeNetworkRequest(TOP_RATED_PATH);
            isFavoritesList = false;
            return true;
        } else if (id == R.id.action_favorites) {
            getSupportLoaderManager().restartLoader(MOVIE_LOADER, null, this);
            isFavoritesList = true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                FavoritesEntry._ID,
                FavoritesEntry.COLUMN_MOVIE_TITLE,
                FavoritesEntry.COLUMN_MOVIE_OVERVIEW,
                FavoritesEntry.COLUMN_MOVIE_RATING,
                FavoritesEntry.COLUMN_MOVIE_POSTER,
                FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE,
                FavoritesEntry.COLUMN_MOVIE_API_ID
        };

        CursorLoader cursorLoader = new CursorLoader(getBaseContext(),
                FavoritesEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        //clear old list of movies
        movies.clear();

        if (cursor.getCount() == 0) {
            showEmptyView(EMPTY_FAVORITES);
        } else {
            noInternetView.setVisibility(View.INVISIBLE);
            moviesGridView.setVisibility(View.VISIBLE);
        }

        //cycle through each row inside cursor
        while (cursor.moveToNext()) {

            //extract data from cursor
            String cursorTitle = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_TITLE));
            String cursorOverview = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_OVERVIEW));
            double cursorRating = cursor.getDouble(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_RATING));
            String cursorPoster = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_POSTER));
            String cursorReleaseDate = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE));
            long cursorId = cursor.getLong(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_API_ID));

            //add to list of movies
            movies.add(new Movie(cursorTitle, cursorOverview, cursorRating, cursorPoster, cursorReleaseDate, cursorId));
        }

        //clean and add new movies to adapter
        refreshAdapter();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMovieArrayAdapter.clear();
    }

    //reloads favorites list if any changes were made
    @Override
    protected void onResume() {
        super.onResume();
        if (isFavoritesList) {
            getSupportLoaderManager().restartLoader(MOVIE_LOADER, null, this);
        }
    }
}
