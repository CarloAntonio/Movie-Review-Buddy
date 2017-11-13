package com.riskitbiskit.moviereviewbuddy.MainActivity;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.riskitbiskit.moviereviewbuddy.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MovieArrayAdapter extends ArrayAdapter<Movie> {

    public static final String POSTER_ROOT = "http://image.tmdb.org/t/p/w500/";

    public MovieArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List movies) {
        super(context, resource, movies);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //Check to see if there is a view available, if yes reuse it, if not inflate a new one
        View itemView = convertView;
        if (itemView == null) {
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.movie_poster_item, parent, false);
        }

        //Get movie from passed in list
        Movie currentMovie = getItem(position);

        String currentTitle = currentMovie.getOriginalTitle();
        String currentOverview = currentMovie.getOverview();
        double currentVoteAverage = currentMovie.getVoteAverage();
        String currentPosterPath = currentMovie.getPosterPath();
        String currentReleaseDate = currentMovie.getReleaseDate();

        //Attach poster to view
        ImageView posterImageView = itemView.findViewById(R.id.poster_iv);
        String fullPosterPath = appendPosterPath(currentPosterPath);
        Picasso.with(getContext()).load(fullPosterPath).into(posterImageView);

        return itemView;
    }

    private String appendPosterPath(String currentPosterPath) {
        return POSTER_ROOT + currentPosterPath;
    }
}
