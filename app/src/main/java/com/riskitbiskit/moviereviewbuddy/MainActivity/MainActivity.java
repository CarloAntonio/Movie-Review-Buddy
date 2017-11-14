package com.riskitbiskit.moviereviewbuddy.MainActivity;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract;
import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.FavoritesEntry;
import com.riskitbiskit.moviereviewbuddy.DetailActivity.DetailsActivity;
import com.riskitbiskit.moviereviewbuddy.R;

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

public class MainActivity extends AppCompatActivity {
    //Testing
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    //Constants
    public static final String API_KEY = "9248410cd68fafd7d26df8b01e1057b0";
    public static final String ROOT_URL = "https://api.themoviedb.org/3";
    public static final String PATH = "path";
    public static final String TOP_RATED_PATH = "/movie/top_rated";
    public static final String MOST_POPULAR_PATH = "/movie/popular";
    //public static final String NOW_PLAYING_PATH = "/movie/now_playing"; //Future Path?
    public static final String ON_SAVE_INSTANCE_STATE_KEY = "key";
    public static final String FAVE_LIST_BOOLEAN = "faveBoolean";
    public static final int MOVIE_LOADER = 0;

    //Bind Views
    @BindView(R.id.no_internet_view)
    TextView noInternetTV;
    @BindView(R.id.content_grid_view)
    GridView moviesGridView;

    private MovieArrayAdapter mMovieArrayAdapter;
    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks;
    private boolean isFavoritesList = false;
    ArrayList<Movie> cursorToList;

    //Test
    OkHttpClient mOkHttpClient;
    List<Movie> movies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //Instantiate new array list
        movies = new ArrayList<>();

        //Setup adapter
        mMovieArrayAdapter = new MovieArrayAdapter(this, R.layout.movie_poster_item, new ArrayList<Movie>());

        if (savedInstanceState != null &&
                savedInstanceState.containsKey(ON_SAVE_INSTANCE_STATE_KEY) &&
                savedInstanceState.containsKey(FAVE_LIST_BOOLEAN)) {

            extractInstanceState(savedInstanceState);

        } else {
            if (getActiveNetworkInfo() != null && getActiveNetworkInfo().isConnected()) {
                makeNetworkCall();
            } else {
                noInternetTV.setText(R.string.no_internet_connectivity);
            }
        }

        moviesGridView.setAdapter(mMovieArrayAdapter);

        moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Get chosen movie
                Movie chosenMovie = mMovieArrayAdapter.getItem(i);

                //Extract traits
                String name = chosenMovie.getOriginalTitle();
                String overview = chosenMovie.getOverview();
                double rating = chosenMovie.getVoteAverage();
                String posterPath = chosenMovie.getPosterPath();
                String releaseDate = chosenMovie.getReleaseDate();
                long id = chosenMovie.getMovieId();

                //Create intent and add traits
                Intent intent = new Intent(getBaseContext(), DetailsActivity.class);
                intent.putExtra(Movie.MOVIE_NAME, name);
                intent.putExtra(Movie.MOVIE_OVERVIEW, overview);
                intent.putExtra(Movie.MOVIE_RATING, rating);
                intent.putExtra(Movie.MOVIE_POSTER_PATH, posterPath);
                intent.putExtra(Movie.MOVIE_RELEASE_DATE, releaseDate);
                intent.putExtra(Movie.MOVIE_ID, id);

                //Start activity
                startActivity(intent);
            }
        });

        mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
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

                cursorToList.clear();

                while (cursor.moveToNext()) {
                    String cursorTitle = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_TITLE));
                    String cursorOverview = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_OVERVIEW));
                    double cursorRating = cursor.getDouble(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_RATING));
                    String cursorPoster = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_POSTER));
                    String cursorReleaseDate = cursor.getString(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE));
                    long cursorId = cursor.getLong(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_API_ID));
                    cursorToList.add(new Movie(cursorTitle, cursorOverview, cursorRating, cursorPoster, cursorReleaseDate, cursorId));
                }

                mMovieArrayAdapter.clear();

                mMovieArrayAdapter.addAll(cursorToList);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mMovieArrayAdapter.clear();
            }
        };

        if (isFavoritesList) {
            getLoaderManager().restartLoader(MOVIE_LOADER, null, mLoaderCallbacks);
            noInternetTV.setText("");
        } else {
            if (getActiveNetworkInfo() != null && getActiveNetworkInfo().isConnected()) {
            } else {
                noInternetTV.setText(R.string.no_internet_connectivity);
            }
        }
    }

    private void extractInstanceState(Bundle savedInstanceState) {
        isFavoritesList = savedInstanceState.getBoolean(FAVE_LIST_BOOLEAN);
        movies = savedInstanceState.getParcelableArrayList(ON_SAVE_INSTANCE_STATE_KEY);
        mMovieArrayAdapter.addAll(movies);
    }

    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    private void makeNetworkCall() {
        mOkHttpClient = new OkHttpClient();

        Uri baseUri = Uri.parse(ROOT_URL + MOST_POPULAR_PATH);
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter("api_key", API_KEY);
        builder.appendQueryParameter("language", "en-US");

        Request request = new Request.Builder()
                .url(builder.toString())
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
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

                movies.add(new Movie(movieTitle, movieOverview, movieRating, moviePosterPath, movieReleaseDate, movieId));
            }

            mMovieArrayAdapter.addAll(movies);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the movie JSON results", e);
        } catch (IOException e) {
            e.printStackTrace();
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_popular) {
//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString(PATH, MOST_POPULAR_PATH);
//            editor.apply();
//            getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
//            isFavoritesList = false;
//            return true;
//        } else if (id == R.id.action_top_rated) {
//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString(PATH, TOP_RATED_PATH);
//            editor.apply();
//            getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
//            isFavoritesList = false;
//            return true;
//        } else if (id == R.id.action_favorites) {
//            getLoaderManager().restartLoader(MOVIE_LOADER, null, mLoaderCallbacks);
//            isFavoritesList = true;
//            noInternetTV.setText("");
//        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {

        //Package relevant info
        outState.putParcelableArrayList(ON_SAVE_INSTANCE_STATE_KEY, (ArrayList<? extends Parcelable>) movies);
        outState.putBoolean(FAVE_LIST_BOOLEAN, isFavoritesList);

        //Clear up array adapter
        mMovieArrayAdapter.clear();

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFavoritesList) {
            getLoaderManager().restartLoader(MOVIE_LOADER, null, mLoaderCallbacks);
        }
    }
}
