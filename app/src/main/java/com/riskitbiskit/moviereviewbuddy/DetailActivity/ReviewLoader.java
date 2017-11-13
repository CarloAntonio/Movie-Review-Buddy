package com.riskitbiskit.moviereviewbuddy.DetailActivity;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.riskitbiskit.moviereviewbuddy.QueryUtils;

import java.util.List;

public class ReviewLoader extends AsyncTaskLoader<List<Review>> {

    private String mUrl;

    public ReviewLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<Review> loadInBackground() {
        if (mUrl == null) return null;

        return QueryUtils.fetchReviewData(mUrl);
    }
}

