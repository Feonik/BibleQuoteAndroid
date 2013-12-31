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
package com.BibleQuote.BibleQuoteAndroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncCommand;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncManager;
import com.BibleQuote.BibleQuoteAndroid.async.command.InitApplication;
import com.BibleQuote.BibleQuoteAndroid.utils.Task;
import com.BibleQuote.R;
import com.BibleQuote.BibleQuoteAndroid.BibleQuoteApp;
import com.BibleQuote.bqtj.android.CoreContextAndroid;
import com.BibleQuote.bqtj.android.utils.LogSysAndroid;
import com.BibleQuote.bqtj.utils.Log;
import com.BibleQuote.bqtj.utils.LogTxt;
import com.BibleQuote.BibleQuoteAndroid.utils.OnTaskCompleteListener;
import com.actionbarsherlock.app.SherlockActivity;

public class SplashActivity extends SherlockActivity implements OnTaskCompleteListener {

	private static final String TAG = "SplashActivity";
	private AsyncCommand initApp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);


		Log.Init(new LogSysAndroid());

		BibleQuoteApp app = (BibleQuoteApp) getApplication();

		LogTxt.Init(app.getCoreContext());

		LogTxt.i(null, "Device model: " + android.os.Build.BRAND + " " + android.os.Build.MODEL);
		LogTxt.i(null, "Device display: " + android.os.Build.BRAND + " " + android.os.Build.DISPLAY);
		LogTxt.i(null, "Android OS: " + android.os.Build.VERSION.RELEASE);
		LogTxt.i(null, "====================================");

		AsyncManager myAsyncManager = app.getAsyncManager();
		if (initApp != null) {
			LogTxt.i(TAG, "Restore old task...");
			myAsyncManager.handleRetainedTask(initApp, this);
		} else {
			LogTxt.i(TAG, "Start task InitApplication...");
			myAsyncManager.setupTask(getTaskObject(), this);
		}
	}

	private AsyncCommand getTaskObject() {
		String progressMessage = getResources().getString(R.string.messageLoad);
		initApp = new AsyncCommand(new InitApplication(this), progressMessage, true);
		return initApp;
	}

	@Override
	public void onTaskComplete(Task task) {
		LogTxt.i(TAG, "Start reader activity");

		BibleQuoteApp app = (BibleQuoteApp) getApplication();

		if (app.isServiceRunning()) {
			startActivity(new Intent(this, ServiceActivity.class));
		} else {
			startActivity(new Intent(this, ReaderActivity.class));
		}

		finish();
	}
}