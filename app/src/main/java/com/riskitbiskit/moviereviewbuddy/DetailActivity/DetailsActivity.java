package com.riskitbiskit.moviereviewbuddy.DetailActivity;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract;
import com.riskitbiskit.moviereviewbuddy.MainActivity.MainActivity;
import com.riskitbiskit.moviereviewbuddy.MainActivity.Movie;
import com.riskitbiskit.moviereviewbuddy.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.riskitbiskit.moviereviewbuddy.Database.FavoritesContract.*;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<String>>,
        View.OnClickListener{

    //Constants
    public static final int PROMO_LOADER = 0;
    public static final int REVIEW_LOADER = 1;
    public static final int ADD_OR_DELETE_LOADER = 2;
    public static final int BUTTON_LOADER = 3;
    public static final String BASE_IMAGE_URL = "http://image.tmdb.org/t/p/w500/";

    //Variables
    private int movieDatabaseId;
    private String movieName;
    private String movieOverview;
    private double movieRating;
    private String moviePosterPath;
    private String movieReleaseDate;
    private long movieId;
    String movieRatingAsString;

    //Views
    @BindView(R.id.loading_error)
    RelativeLayout relativeLayout;
    @BindView(R.id.title_tv)
    TextView titleTV;
    @BindView(R.id.overview_tv)
    TextView overviewTV;
    @BindView(R.id.rating_tv)
    TextView ratingTV;
    @BindView(R.id.poster_iv)
    ImageView posterIV;
    @BindView(R.id.release_tv)
    TextView releaseTV;
    @BindView(R.id.trailers_table_layout)
    TableLayout trailerTableLayout;
    @BindView(R.id.review_table_layout)
    TableLayout reviewTableLayout;
    @BindView(R.id.add_to_faves_button)
    Button favesButton;

    //Loaders
    LoaderManager.LoaderCallbacks reviewLoaderCallback;
    LoaderManager.LoaderCallbacks addOrDeleteCallback;
    LoaderManager.LoaderCallbacks buttonCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);

        //Get intent details
        Intent intent = getIntent();
        if (intent != null
                && intent.hasExtra(Movie.MOVIE_NAME)
                && intent.hasExtra(Movie.MOVIE_OVERVIEW) && intent.hasExtra(Movie.MOVIE_RATING)
                && intent.hasExtra(Movie.MOVIE_POSTER_PATH) && intent.hasExtra(Movie.MOVIE_RELEASE_DATE)
                && intent.hasExtra(Movie.MOVIE_ID)) {

            relativeLayout.setVisibility(View.INVISIBLE);

            movieName = intent.getStringExtra(Movie.MOVIE_NAME);
            movieOverview = intent.getStringExtra(Movie.MOVIE_OVERVIEW);
            movieRating = intent.getDoubleExtra(Movie.MOVIE_RATING, 0.0);
            movieRatingAsString = String.valueOf(movieRating);
            moviePosterPath = intent.getStringExtra(Movie.MOVIE_POSTER_PATH);
            movieReleaseDate = intent.getStringExtra(Movie.MOVIE_RELEASE_DATE);
            movieId = intent.getLongExtra(Movie.MOVIE_ID, 0);

            //Set views
            titleTV.setText(movieName);
            overviewTV.setText(movieOverview);
            ratingTV.setText(movieRatingAsString);
            releaseTV.setText(movieReleaseDate);

            //Set image to view
            String fullUrl = BASE_IMAGE_URL + moviePosterPath;
            Picasso.with(this).load(fullUrl).into(posterIV);

            //Locate favorites button
            favesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getLoaderManager().initLoader(ADD_OR_DELETE_LOADER, null, addOrDeleteCallback);
                }
            });

        } else {
            relativeLayout.setVisibility(View.VISIBLE);
        }

        addOrDeleteCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader onCreateLoader(int i, Bundle bundle) {
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
            public void onLoadFinished(Loader loader, Cursor cursor) {
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
            public void onLoaderReset(Loader loader) {

            }
        };

        reviewLoaderCallback = new LoaderManager.LoaderCallbacks<List<Review>>() {
            @Override
            public Loader<List<Review>> onCreateLoader(int i, Bundle bundle) {
                Uri baseUri = Uri.parse(MainActivity.ROOT_URL + "/movie/" + movieId + "/reviews");
                Uri.Builder builder = baseUri.buildUpon();

                builder.appendQueryParameter("api_key", MainActivity.API_KEY);
                builder.appendQueryParameter("language", "en-US");
                builder.appendQueryParameter("page", "1");

                return new ReviewLoader(getBaseContext(), builder.toString());
            }

            @Override
            public void onLoadFinished(Loader<List<Review>> loader, List<Review> reviews) {
                View tableRow;
                if (!reviews.isEmpty()) {
                    //Create a new row for each video path
                    for (int i = 0; i < reviews.size(); i++) {
                        tableRow = View.inflate(getBaseContext(), R.layout.review_table_row, null);
                        TextView contentTextView = tableRow.findViewById(R.id.review_content_tv);
                        TextView authorTextView = tableRow.findViewById(R.id.review_author_tv);

                        //set text
                        contentTextView.setText(reviews.get(i).getContent());
                        authorTextView.setText("-" + reviews.get(i).getAuthor());

                        reviewTableLayout.addView(tableRow);
                    }
                }
            }

            @Override
            public void onLoaderReset(Loader<List<Review>> loader) {
                reviewTableLayout.removeAllViews();
            }
        };

        buttonCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader onCreateLoader(int i, Bundle bundle) {
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

        getLoaderManager().initLoader(BUTTON_LOADER, null, buttonCallback);
        getLoaderManager().initLoader(PROMO_LOADER, null, this);
        getLoaderManager().initLoader(REVIEW_LOADER, null, reviewLoaderCallback);
    }

    @Override
    public Loader<List<String>> onCreateLoader(int i, Bundle bundle) {
        Uri baseUri = Uri.parse(MainActivity.ROOT_URL + "/movie/" + movieId + "/videos");
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter("api_key", MainActivity.API_KEY);
        builder.appendQueryParameter("language", "en-US");

        return new VideoPathLoader(this, builder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> videoPaths) {
        View tableRow;
        if (!videoPaths.isEmpty()) {
            //Create a new row for each video path
            for (int i = 0; i < videoPaths.size(); i++) {
                tableRow = View.inflate(getBaseContext(), R.layout.trailer_table_row, null);
                tableRow.setTag(videoPaths.get(i));
                tableRow.setOnClickListener(this);
                TextView textView = tableRow.findViewById(R.id.trailer_tv);
                int trailerNumber = i + 1;
                textView.setText("Play Movie Trailer " + trailerNumber);
                trailerTableLayout.addView(tableRow);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
        trailerTableLayout.removeAllViews();
    }

    @Override
    public void onClick(View view) {
        String identifier = (String) view.getTag();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + identifier)));
        trailerTableLayout.removeAllViews();
        reviewTableLayout.removeAllViews();
    }
}
