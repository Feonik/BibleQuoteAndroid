package com.BibleQuote.BibleQuoteAndroid.async;

import com.BibleQuote.bqtj.utils.Log;
import com.BibleQuote.BibleQuoteAndroid.utils.Task;

public class AsyncWait extends Task {
	private final String TAG = "AsyncWait";
	private AsyncTaskManager currentAsyncTaskManager;


	public AsyncWait(String message, Boolean isHidden, AsyncTaskManager currentAsyncTaskManager) {
		super(message, isHidden);
		this.currentAsyncTaskManager = currentAsyncTaskManager;
	}


	@Override
	protected Boolean doInBackground(String... arg0) {
		try {
			if (currentAsyncTaskManager != null) {
				while (true) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						break;
					}
					if (!currentAsyncTaskManager.isWorking()) {
						break;
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		return true;
	}


	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
	}


}
