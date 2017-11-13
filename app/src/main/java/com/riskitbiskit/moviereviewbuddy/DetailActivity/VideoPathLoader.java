package com.riskitbiskit.moviereviewbuddy.DetailActivity;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.riskitbiskit.moviereviewbuddy.QueryUtils;

import java.util.List;

public class VideoPathLoader extends AsyncTaskLoader<List<String>> {

    private String mUrl;

    public VideoPathLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<String> loadInBackground() {
        if (mUrl == null) return null;

        return QueryUtils.fetchVideoPath(mUrl);
    }
}
