package com.riskitbiskit.moviereviewbuddy.MainActivity;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.riskitbiskit.moviereviewbuddy.QueryUtils;

import java.util.List;

public class MovieLoader extends AsyncTaskLoader<List<Movie>> {

    private String mUrl;

    public MovieLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<Movie> loadInBackground() {
        if (mUrl == null) return null;

        return QueryUtils.fetchMovies(mUrl);
    }
}
