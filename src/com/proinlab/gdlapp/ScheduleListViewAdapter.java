
package com.proinlab.gdlapp;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
class ScheduleListViewAdapter extends BaseAdapter implements OnClickListener {

	private Context mContext;
    private LayoutInflater Inflater;
    private ArrayList<ArrayList<String>> arSrc;
    private int layout;
    private LruCache<Integer, Bitmap> bitmapCache;
    private final SimpleDateFormat sdfWeb = new SimpleDateFormat("dd MMM yyyy HH:mm:ss.S Z", Locale.ENGLISH);
    private final SimpleDateFormat sdfLocal = new SimpleDateFormat("MMM dd, yyyy, hh:mm aa", Locale.getDefault());

    public static final int ARRAY_INDEX_TITLE = 0;
    public static final int ARRAY_INDEX_DATE = 1;
    public static final int ARRAY_INDEX_THUMBNAIL = 2;
    public static final int ARRAY_INDEX_URL = 3;

    public ScheduleListViewAdapter(Context context,
            ArrayList<ArrayList<String>> aarSrc) {
        Inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        arSrc = aarSrc;
        layout = R.layout.schedule_content;
        mContext = context;
        
        bitmapCache = new LruCache<Integer, Bitmap>(10 * 1024 * 1024) {
            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public int getCount() {
        return arSrc.size();
    }

    public ArrayList<String> getItem(int position) {
        return arSrc.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("NewApi")
	public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = Inflater.inflate(layout, parent, false);
        }

        ImageView thumbnail = (ImageView) convertView
                .findViewById(R.id.schedule_content_thumbnails);
        thumbnail.setImageBitmap(null);

        TextView title = (TextView) convertView
                .findViewById(R.id.schedule_content_name);
        title.setText(arSrc.get(position).get(ARRAY_INDEX_TITLE));

        TextView date = (TextView) convertView
                .findViewById(R.id.schedule_content_date);
        try {
        	Date eventDate = sdfWeb.parse(arSrc.get(position).get(ARRAY_INDEX_DATE));
        	date.setText(sdfLocal.format(eventDate));
        } catch (ParseException e) {
        	date.setText(arSrc.get(position).get(ARRAY_INDEX_DATE));
        }
        
        date.setTag(position);
        date.setOnClickListener(createEventReminderClickListener);

        convertView.setTag(position);
        convertView.setOnClickListener(this);

        final ThumbnailAsyncTask oldTask = (ThumbnailAsyncTask) thumbnail.getTag();
		if (oldTask != null) {
			oldTask.cancel(false);
		}

		if (bitmapCache != null) {
			final Bitmap cachedResult = bitmapCache.get(position);
			if (cachedResult != null) {
				thumbnail.setImageBitmap(cachedResult);
				thumbnail.setBackgroundDrawable(null);
				return convertView;
			}
		}
		
		final ThumbnailAsyncTask task = new ThumbnailAsyncTask(thumbnail);
		thumbnail.setTag(task);
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, position);
			} else {
				task.execute(position);
			}
		} catch (RejectedExecutionException e) {
		} catch (OutOfMemoryError e) {

		}
        
        return convertView;
    }

	private final OnClickListener createEventReminderClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int position = (Integer) v.getTag();
			
			try {
				Date eventDate = sdfWeb.parse(arSrc.get(position).get(ARRAY_INDEX_DATE));
				Calendar cal = Calendar.getInstance(TimeZone.getDefault());
				cal.setTime(eventDate);

				Intent intent = new Intent(Intent.ACTION_EDIT);
				intent.setType("vnd.android.cursor.item/event");
				intent.putExtra("beginTime", cal.getTimeInMillis());
				intent.putExtra("allDay", false);
				intent.putExtra("endTime",
						cal.getTimeInMillis() + 60 * 60 * 1000);
				intent.putExtra("title",
						arSrc.get(position).get(ARRAY_INDEX_TITLE));

				mContext.startActivity(intent);
			} catch (ParseException e) {
			}
		}

	};
    
	private class ThumbnailAsyncTask extends AsyncTask<Integer, Void, Bitmap> {
		private final ImageView mTarget;

		public ThumbnailAsyncTask(ImageView target) {
			mTarget = target;
		}

		@Override
		protected void onPreExecute() {
			mTarget.setTag(this);
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			final int pos = params[0];
			final String url = arSrc.get(pos).get(ARRAY_INDEX_THUMBNAIL);

			try {
				InputStream is = new URL(url).openStream();
				final Bitmap result = BitmapFactory.decodeStream(is);
				is.close();
				
				if (bitmapCache != null && result != null) {
					bitmapCache.put(pos, result);
				}
				
				return result;
			} catch (Exception e) {

			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (mTarget.getTag() == this) {
				mTarget.setImageBitmap(result);
				mTarget.setBackgroundDrawable(null);
				mTarget.setTag(null);
			}
		}
	}
    
    @Override
    public void onClick(View v) {
    	int position = (Integer) v.getTag();
    	
    	String url = arSrc.get(position).get(ARRAY_INDEX_URL);    	
    	Intent intent = new Intent(Intent.ACTION_VIEW);
    	intent.setData(Uri.parse(url));
    	mContext.startActivity(intent);
    }

}
