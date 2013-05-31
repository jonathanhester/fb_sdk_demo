package com.jonathanhester.fb_demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotosActivity extends Activity {

	protected static JSONArray jsonArray;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photos);

		Bundle extras = getIntent().getExtras();
		String apiResponse = extras.getString("API_RESPONSE");
		try {
			jsonArray = new JSONObject(apiResponse).getJSONArray("data");
		} catch (JSONException e) {
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG)
					.show();
			return;
		}

		GridView g = (GridView) findViewById(R.id.myGrid);
		g.setAdapter(new ImageAdapter(this));
	}

	/**
	 * Definition of the list adapter
	 */
	public class ImageAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		PhotosActivity photoActivity;

		public ImageAdapter(PhotosActivity photoActivity) {
			this.photoActivity = photoActivity;
			if (Utility.model == null) {
				Utility.model = new ImageDownloader();
			}
			Utility.model.setListener(this);
			mInflater = LayoutInflater.from(photoActivity.getBaseContext());
		}

		@Override
		public int getCount() {
			return jsonArray.length();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			JSONObject jsonObject = null;
			try {
				jsonObject = jsonArray.getJSONObject(position);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			View hView = convertView;
			if (convertView == null) {
				hView = mInflater.inflate(R.layout.grid_image, null);
				ViewHolder holder = new ViewHolder();
				holder.image = (ImageView) hView
						.findViewById(R.id.feed_pic);
				hView.setTag(holder);
			}

			ViewHolder holder = (ViewHolder) hView.getTag();
			try {
				String picUrl = jsonObject.getString("picture");
				holder.image.setImageBitmap(Utility.model.getImage(
						jsonObject.getString("id"), picUrl));
			} catch (JSONException e) {
			}
			return hView;
		}

	}

	class ViewHolder {
		ImageView image;
	}

}
