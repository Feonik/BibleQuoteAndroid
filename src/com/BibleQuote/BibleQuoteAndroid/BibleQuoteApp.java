/*
 * Copyright (C) 2011 Scripture Software (http://scripturesoftware.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.BibleQuote.BibleQuoteAndroid;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncManager;
import com.BibleQuote.BibleQuoteAndroid.services.BibleQuoteService;
import com.BibleQuote.bqtj.CoreContext;
import com.BibleQuote.bqtj.android.CoreContextAndroid;
import com.BibleQuote.bqtj.managers.bookmarks.repository.IBookmarksRepository;
//import com.BibleQuote.bqtj.managers.bookmarks.repository.dbBookmarksRepository;


public class BibleQuoteApp extends Application {

	private static final String TAG = "BibleQuoteApp";

	private AsyncManager mAsyncManager = null;


	@Override
	public void onCreate() {
		super.onCreate();
	}

	public CoreContext getCoreContext() {
		return CoreContextAndroid.getCoreContextAndroid(this);
	}

	public AsyncManager getAsyncManager() {
		if (mAsyncManager == null) {
			mAsyncManager = new AsyncManager();
		}
		return mAsyncManager;
	}

	public boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (BibleQuoteService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
