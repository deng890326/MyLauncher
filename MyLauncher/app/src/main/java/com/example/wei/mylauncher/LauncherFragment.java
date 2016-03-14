package com.example.wei.mylauncher;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LauncherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LauncherFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private AppAdapter mAdapter;

    private ThumbnailLoader<AppViewHolder> mThumbnailLoader;


    public LauncherFragment() {
        // Required empty public constructor
    }

    public static LauncherFragment newInstance() {
        LauncherFragment fragment = new LauncherFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThumbnailLoader = new ThumbnailLoader<>(getActivity(), new Handler(),
                new ThumbnailLoader.OnRequestDoneListener<AppViewHolder>() {
                    @Override
                    public void onRequestDone(AppViewHolder target, Pair<Drawable, CharSequence> result) {
                        if (result != null && isResumed()) {
                            target.mImageView.setImageDrawable(result.first);
                            target.mTextView.setText(result.second);
                        }
                    }
                });
        mThumbnailLoader.start();
        mThumbnailLoader.getLooper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_launcher, container, false);

        PackageManager pm = getActivity().getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appInfos = pm.queryIntentActivities(launcherIntent, 0);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new AppAdapter(appInfos);
        mRecyclerView.setAdapter(mAdapter);

        return v;
    }

    private class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ResolveInfo mResolveInfo;
        private TextView mTextView;
        private ImageView mImageView;

        public AppViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.name);
            itemView.setOnClickListener(this);
            mImageView = (ImageView) itemView.findViewById(R.id.thumbnail);
        }

        public void bindInfo(ResolveInfo info) {
            mResolveInfo = info;
//            PackageManager pm = getActivity().getPackageManager();
//            mTextView.setText(info.loadLabel(pm));
            if (!mThumbnailLoader.isCached(info)) {
                mImageView.setImageResource(R.drawable.android);
                mTextView.setText(R.string.loading);
            }
            mThumbnailLoader.queueThumbnail(this, info);
        }

        @Override
        public void onClick(View v) {
            ActivityInfo activityInfo = mResolveInfo.activityInfo;

            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(activityInfo.packageName, activityInfo.name)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private class AppAdapter extends RecyclerView.Adapter<AppViewHolder> {

        private List<ResolveInfo> mAppInfos;

        public AppAdapter(List<ResolveInfo> appInfos) {
            mAppInfos = appInfos;
        }

        @Override
        public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            return new AppViewHolder(inflater.inflate(R.layout.list_app_view, parent, false));
        }

        @Override
        public void onBindViewHolder(AppViewHolder holder, int position) {
            holder.bindInfo(mAppInfos.get(position));
        }

        @Override
        public int getItemCount() {
            return mAppInfos.size();
        }
    }

}
