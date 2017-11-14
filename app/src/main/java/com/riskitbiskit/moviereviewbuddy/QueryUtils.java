package com.riskitbiskit.moviereviewbuddy;

import android.text.TextUtils;
import android.util.Log;

import com.riskitbiskit.moviereviewbuddy.DetailActivity.Review;
import com.riskitbiskit.moviereviewbuddy.MainActivity.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class QueryUtils {
    public static final String LOG_TAG = QueryUtils.class.getSimpleName();

    private QueryUtils() {}

    //Fetch a list of movies
    public static List<Movie> fetchMovies(String requestUrl) {
        //transform String form of Url to actual url
        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException IOE) {
            Log.e(LOG_TAG, "Error closing input stream", IOE);
        }

        return extractMovies(jsonResponse);
    }

    //Fetch a list of user reviews
    public static List<Review> fetchReviewData (String requestUrl) {
        //transform String form of Url to actual url
        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException IOE) {
            Log.e(LOG_TAG, "Error closing input stream", IOE);
        }

        return extractReviews(jsonResponse);
    }

    public static List<String> fetchVideoPath (String requestUrl) {
        //transform String form of Url to actual url
        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException IOE) {
            Log.e(LOG_TAG, "Error closing input stream", IOE);
        }

        return extractVideoPath(jsonResponse);
    }

    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the movie JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private static List<Movie> extractMovies(String jsonResponse) {

        if (TextUtils.isEmpty(jsonResponse)) {
            return null;
        }

        List<Movie> movies = new ArrayList<>();

        try {
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
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the movie JSON results", e);
        }
        return movies;
    }

    private static List<Review> extractReviews(String jsonResponse) {
        if (TextUtils.isEmpty(jsonResponse)) {
            return null;
        }

        List<Review> reviews = new ArrayList<>();

        try {
            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONArray resultsArray = rootObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentMovieReview = resultsArray.getJSONObject(i);

                String author = currentMovieReview.getString("author");
                String content = currentMovieReview.getString("content");

                reviews.add(new Review(author, content));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the movie reviews JSON results", e);
        }

        return reviews;
    }

    private static List<String> extractVideoPath(String jsonResponse) {
        if (TextUtils.isEmpty(jsonResponse)) {
            return null;
        }

        List<String> videoPaths = new ArrayList<>();

        try {
            JSONObject rootObject = new JSONObject(jsonResponse);
            JSONArray resultsArray = rootObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentMovieReview = resultsArray.getJSONObject(i);

                String currentMoviePath = currentMovieReview.getString("key");

                videoPaths.add(currentMoviePath);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the promo video JSON results", e);
        }
        return videoPaths;
    }
}