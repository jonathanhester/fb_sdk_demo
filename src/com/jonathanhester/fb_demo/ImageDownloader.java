/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jonathanhester.fb_demo;

import java.util.Hashtable;
import java.util.Stack;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.BaseAdapter;

/*
 * Fetch friends profile pictures request via AsyncTask
 */
public class ImageDownloader {

	Hashtable<String, Bitmap> friendsImages;
	Hashtable<String, String> positionRequested;
	BaseAdapter listener;
	int runningCount = 0;
	Stack<ItemPair> queue;

	/*
	 * 15 max async tasks at any given time.
	 */
	final static int MAX_ALLOWED_TASKS = 15;

	public ImageDownloader() {
		friendsImages = new Hashtable<String, Bitmap>();
		positionRequested = new Hashtable<String, String>();
		queue = new Stack<ItemPair>();
	}

	/*
	 * Inform the listener when the image has been downloaded. listener is
	 * FriendsList here.
	 */
	public void setListener(BaseAdapter listener) {
		this.listener = listener;
		reset();
	}

	public void reset() {
		positionRequested.clear();
		runningCount = 0;
		queue.clear();
	}

	/*
	 * If the profile picture has already been downloaded and cached, return it
	 * else execute a new async task to fetch it - if total async tasks >15,
	 * queue the request.
	 */
	public Bitmap getImage(String id, String url) {
		Bitmap image = friendsImages.get(id);
		if (image != null) {
			return image;
		}
		if (!positionRequested.containsKey(id)) {
			positionRequested.put(id, "");
			if (runningCount >= MAX_ALLOWED_TASKS) {
				queue.push(new ItemPair(id, url));
			} else {
				runningCount++;
				new ImageDownloadAsyncTask().execute(id, url);
			}
		}
		return null;
	}

	public void getNextImage() {
		if (!queue.isEmpty()) {
			ItemPair item = queue.pop();
			new ImageDownloadAsyncTask().execute(item.id, item.url);
		}
	}

	/*
	 * Start a AsyncTask to fetch the request
	 */
	private class ImageDownloadAsyncTask extends
			AsyncTask<Object, Void, Bitmap> {
		String id;

		@Override
		protected Bitmap doInBackground(Object... params) {
			this.id = (String) params[0];
			String url = (String) params[1];
			return Utility.getBitmap(url);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			runningCount--;
			if (result != null) {
				friendsImages.put(id, result);
				listener.notifyDataSetChanged();
				getNextImage();
			}
		}
	}

	class ItemPair {
		String id;
		String url;

		public ItemPair(String id, String url) {
			this.id = id;
			this.url = url;
		}
	}

}
