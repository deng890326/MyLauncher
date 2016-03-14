package com.example.wei.mylauncher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by wei on 2016/2/28 0028.
 */
public class ThumbnailLoader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailLoader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mResponder;
    private Handler mRequestHandler;
    private Context mContext;
    private OnRequestDoneListener<T> mOnRequestDoneListener;
    private ConcurrentMap<T, ResolveInfo> mRequestMap = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<Pair<T, ResolveInfo>> mQueue = new LinkedBlockingQueue<>();
    private LruCache<ResolveInfo, Pair<Drawable, CharSequence>> mCache = new LruCache<>(100);

    public ThumbnailLoader(Context context, Handler responder,
                           OnRequestDoneListener<T> onRequestDoneListener) {
        super(TAG);
        mResponder = responder;
        mOnRequestDoneListener = onRequestDoneListener;
        mContext = context;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                   handleRequest((T) msg.obj);
                }
            }
        };

        Pair<T, ResolveInfo> pair;
        while ((pair = mQueue.poll()) != null) {
            queueThumbnail(pair.first, pair.second);
        }
        mQueue = null;
    }

    public boolean isCached(ResolveInfo info) {
        return mCache.get(info) != null;
    }

    public void queueThumbnail(T target, ResolveInfo info) {
        if (mRequestHandler == null) {
            try {
                mQueue.put(new Pair<>(target, info));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        Log.d(TAG, "queueThumbnail, target=" + target + ", info=" + info);
        mRequestMap.remove(target);
        mResponder.removeMessages(MESSAGE_DOWNLOAD, target);
        if (info != null) {
            Pair<Drawable, CharSequence> cache = mCache.get(info);
            if (cache == null) {
                mRequestMap.put(target, info);
                mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
            } else {
                if (mOnRequestDoneListener != null) {
                    mOnRequestDoneListener.onRequestDone(target, cache);
                }
            }
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target) {
        Log.d(TAG, "handleRequest, target=" + target);
        final ResolveInfo info = mRequestMap.get(target);

        PackageManager pm = mContext.getPackageManager();
        Drawable drawable = info.loadIcon(pm);
        CharSequence text = info.loadLabel(pm);

        final Pair<Drawable, CharSequence> pair = Pair.create(drawable, text);
        if (pair != null) {
            mCache.put(info, pair);
        }

        mResponder.post(new Runnable() {
            @Override
            public void run() {
                if (!mRequestMap.get(target).equals(info)) {
                    return;
                }
                mRequestMap.remove(target);
                mOnRequestDoneListener.onRequestDone(target, pair);
            }
        });

    }

    public interface OnRequestDoneListener<T> {
        void onRequestDone(T target, Pair<Drawable, CharSequence> result);
    }
}
