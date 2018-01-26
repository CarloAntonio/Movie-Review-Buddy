package com.riskitbiskit.moviereviewbuddy.presenter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.riskitbiskit.moviereviewbuddy.model.Movie;
import com.riskitbiskit.moviereviewbuddy.R;
import com.riskitbiskit.moviereviewbuddy.model.Review;
import com.squareup.picasso.Picasso;

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

import static com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.*;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener{

    //Testing
    public static final String LOG_TAG = DetailsActivity.class.getSimpleName();

    //Constants
    public static final int ADD_OR_DELETE_LOADER = 2;
    public static final int BUTTON_LOADER = 3;
    public static final String BASE_IMAGE_URL = "http://image.tmdb.org/t/p/w500/";
    public static final String REVIEW_SAVE_INS_KEY = "review_key";
    public static final String TRAILER_SAVE_INS_KEY = "trailer_key";
    public static final String MOVIE_SAVE_INS_KEY = "movie_key";

    private Movie mMovie;
    private String movieName;
    private String movieOverview;
    private double movieRating;
    private String moviePosterPath;
    private String movieReleaseDate;
    private long movieId;
    OkHttpClient mOkHttpClient;
    Context mContext = this;
    List<Review> mReviews;
    List<String> mVideoPaths;

    //Views
    @BindView(R.id.main_content_sv) ScrollView mScrollView;
    @BindView(R.id.errorTV) TextView mErrorTV;
    @BindView(R.id.title_tv) TextView titleTV;
    @BindView(R.id.overview_tv) TextView overviewTV;
    @BindView(R.id.rating_tv) TextView ratingTV;
    @BindView(R.id.poster_iv) ImageView posterIV;
    @BindView(R.id.release_tv) TextView releaseTV;
    @BindView(R.id.trailers_table_layout) TableLayout trailerTableLayout;
    @BindView(R.id.review_table_layout) TableLayout reviewTableLayout;
    @BindView(R.id.add_to_faves_button) Button favesButton;
    @BindView(R.id.reviews_section) LinearLayout mReviewSectionLL;
    @BindView(R.id.trailer_section) LinearLayout mTrailerSectionLL;

    //Loaders
    LoaderManager.LoaderCallbacks buttonCallback;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //save review data
        outState.putParcelableArrayList(REVIEW_SAVE_INS_KEY, (ArrayList<? extends Parcelable>) mReviews);
        //save trailer data
        outState.putStringArrayList(TRAILER_SAVE_INS_KEY, (ArrayList<String>) mVideoPaths);
        //save movie
        outState.putParcelable(MOVIE_SAVE_INS_KEY, mMovie);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);

        //check for saved data
        if (savedInstanceState != null) {
            //check if review data was previously saved
            if (savedInstanceState.containsKey(REVIEW_SAVE_INS_KEY)) {
                mReviews = savedInstanceState.getParcelableArrayList(REVIEW_SAVE_INS_KEY);
                if (mReviews == null || mReviews.size() <= 0) {
                    mReviewSectionLL.setVisibility(View.GONE);
                } else {
                    createReviewRows();
                }
            }

            if (savedInstanceState.containsKey(TRAILER_SAVE_INS_KEY)) {
                mVideoPaths = savedInstanceState.getStringArrayList(TRAILER_SAVE_INS_KEY);
                if (mVideoPaths == null || mVideoPaths.size() <= 0) {
                    mTrailerSectionLL.setVisibility(View.GONE);
                } else {
                    createTrailerRows();
                }
            }

            mMovie = savedInstanceState.getParcelable(MOVIE_SAVE_INS_KEY);

            setupGeneralDateViews(mMovie);

            //if no previous review or trailer data therefore, make a network call
        } else {

            //get intent
            Intent intent = getIntent();
            mMovie = intent.getParcelableExtra(Movie.MOVIE);

            setupGeneralDateViews(mMovie);

            //initialize OkHttpClient
            mOkHttpClient = new OkHttpClient();
            //make necessary network calls
            makeReviewNetworkCall();
            makePromoTrailerCall();
        }

        //Locate favorites button
        favesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportLoaderManager().initLoader(ADD_OR_DELETE_LOADER, null, DetailsActivity.this);
            }
        });

        createButtonCallback();

        getSupportLoaderManager().initLoader(BUTTON_LOADER, null, buttonCallback);
    }

    private void setupGeneralDateViews(Movie movie) {
        //extract data from intent
        movieName = movie.getOriginalTitle();
        movieOverview = movie.getOverview();
        movieRating = movie.getVoteAverage();
        moviePosterPath = movie.getPosterPath();
        movieReleaseDate = movie.getReleaseDate();
        movieId = movie.getMovieId();

        //Set views
        titleTV.setText(movieName);
        overviewTV.setText(movieOverview);
        ratingTV.setText(String.valueOf(movieRating));
        releaseTV.setText(movieReleaseDate);

        //Set image to view
        String fullUrl = BASE_IMAGE_URL + moviePosterPath;
        Picasso.with(this).load(fullUrl).into(posterIV);
    }

    private void makeReviewNetworkCall() {

        //Create full uri for review
        Uri baseUri = Uri.parse(MainActivity.ROOT_URL + "/movie/" + movieId + "/reviews");
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter("api_key", MainActivity.API_KEY);
        builder.appendQueryParameter("language", "en-US");
        builder.appendQueryParameter("page", "1");

        //create request
        Request request = new Request.Builder()
                .url(builder.toString())
                .build();

        //make request call
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(LOG_TAG, "Error requesting review data from server");
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processReviewResponse(response);
                    }
                });
            }
        });
    }

    private void processReviewResponse(Response response) {

        //create a new list of reviews
        mReviews = new ArrayList<>();
        JSONArray resultsArray = null;

        //process response
        try {
            //transform response to string form
            String jsonResponse = response.body().string();

            //parse through json response
            JSONObject rootObject = new JSONObject(jsonResponse);
            resultsArray = rootObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentMovieReview = resultsArray.getJSONObject(i);

                String author = currentMovieReview.getString("author");
                String content = currentMovieReview.getString("content");

                mReviews.add(new Review(author, content));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing reviews from JSON results", e);
        } catch (IOException IOE) {
            Log.e(LOG_TAG, "Problem with I/O", IOE);
        }

        //check to see if there are any reviews
        if (resultsArray == null || resultsArray.length() <= 0 ) {
           //if not remove reviews section
            mReviewSectionLL.setVisibility(View.GONE);
        } else  {
            //create rows for each review item
            createReviewRows();
        }
    }

    private void createReviewRows() {
        //make review section visible
        mReviewSectionLL.setVisibility(View.VISIBLE);

        View tableRow;
        if (!mReviews.isEmpty()) {
            //Create a new row for each video path
            for (int i = 0; i < mReviews.size(); i++) {
                tableRow = View.inflate(getBaseContext(), R.layout.review_table_row, null);
                TextView contentTextView = tableRow.findViewById(R.id.review_content_tv);
                TextView authorTextView = tableRow.findViewById(R.id.review_author_tv);

                //set text
                contentTextView.setText(mReviews.get(i).getContent());
                authorTextView.setText("-" + mReviews.get(i).getAuthor());

                reviewTableLayout.addView(tableRow);
            }
        }
    }

    private void makePromoTrailerCall() {

        //Create full uri for promo
        Uri baseUri = Uri.parse(MainActivity.ROOT_URL + "/movie/" + movieId + "/videos");
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter("api_key", MainActivity.API_KEY);
        builder.appendQueryParameter("language", "en-US");

        //create request
        Request request = new Request.Builder()
                .url(builder.toString())
                .build();

        //make request call
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(LOG_TAG, "Error requesting trailer data from server");
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processTrailerResponse(response);
                    }
                });
            }
        });
    }

    private void processTrailerResponse(Response response) {

        //create new list of video paths
        mVideoPaths = new ArrayList<>();
        JSONArray resultsArray = null;

        try {
            String jsonResponse = response.body().string();

            JSONObject rootObject = new JSONObject(jsonResponse);
            resultsArray = rootObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentMovieReview = resultsArray.getJSONObject(i);

                String currentMoviePath = currentMovieReview.getString("key");

                mVideoPaths.add(currentMoviePath);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the promo video JSON results", e);
        } catch (IOException IOE) {
            Log.e(LOG_TAG, "Problem with I/O for trailer data", IOE);
        }

        //check to see if there are any reviews
        if (resultsArray == null || resultsArray.length() <= 0 ) {
            //if not remove reviews section
            mTrailerSectionLL.setVisibility(View.GONE);
        } else  {
            //create rows for each trailer item
            createTrailerRows();
        }
    }

    private void createTrailerRows() {
        //make trailer section visible
        mTrailerSectionLL.setVisibility(View.VISIBLE);

        View tableRow;
        if (!mVideoPaths.isEmpty()) {
            //Create a new row for each video path
            for (int i = 0; i < mVideoPaths.size(); i++) {
                tableRow = View.inflate(getBaseContext(), R.layout.trailer_table_row, null);
                tableRow.setTag(mVideoPaths.get(i));
                tableRow.setOnClickListener(this);
                TextView textView = tableRow.findViewById(R.id.trailer_tv);
                int trailerNumber = i + 1;
                textView.setText("Play Movie Trailer " + trailerNumber);

                trailerTableLayout.addView(tableRow);
            }
        }
    }

    private void createButtonCallback() {
        buttonCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader onCreateLoader(int id, Bundle args) {
                String[] projection = {
                        FavoritesEntry._ID,
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
                boolean isUnique = true;

                while (cursor.moveToNext()) {
                    if (cursor.getLong(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_API_ID)) == movieId) {
                        isUnique = false;
                    }
                }

                if (!isUnique) {
                    Button favesButton = (Button) findViewById(R.id.add_to_faves_button);
                    favesButton.setText("Unfavorite");
                }
            }

            @Override
            public void onLoaderReset(Loader loader) {

            }
        };
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                FavoritesEntry._ID,
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
        boolean isUnique = true;
        int copyId = -1;

        while (cursor.moveToNext()) {
            if (cursor.getLong(cursor.getColumnIndex(FavoritesEntry.COLUMN_MOVIE_API_ID)) == movieId) {
                isUnique = false;
                copyId = cursor.getInt(cursor.getColumnIndex(FavoritesEntry._ID));
            }
        }

        if (isUnique) {
            ContentValues values = new ContentValues();
            values.put(FavoritesEntry.COLUMN_MOVIE_TITLE, movieName);
            values.put(FavoritesEntry.COLUMN_MOVIE_OVERVIEW, movieOverview);
            values.put(FavoritesEntry.COLUMN_MOVIE_RATING, movieRating);
            values.put(FavoritesEntry.COLUMN_MOVIE_POSTER, moviePosterPath);
            values.put(FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE, movieReleaseDate);
            values.put(FavoritesEntry.COLUMN_MOVIE_API_ID, movieId);

            Uri addFaveUri = getContentResolver().insert(FavoritesEntry.CONTENT_URI, values);
            // Show a toast message depending on whether or not the insertion was successful
            if (addFaveUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(getBaseContext(), "Error saving movie to favorites", Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(getBaseContext(), "Movie added to favorites", Toast.LENGTH_SHORT).show();
            }

            finish();
        } else {
            Uri selectedFaveUri = ContentUris.withAppendedId(FavoritesEntry.CONTENT_URI, copyId);
            int rowsDeleted = getContentResolver().delete(
                    selectedFaveUri,
                    null,
                    null
            );

            if (rowsDeleted > 0) {
                Toast.makeText(getBaseContext(), "Movie Deleted From Favorites", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "Error Deleting Movie From Favorites", Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //Empty
    }

    @Override
    public void onClick(View view) {
        String identifier = (String) view.getTag();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + identifier)));
    }
}
