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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.BibleQuote.BibleQuoteAndroid.ui.fragments.TTSPlayerFragment;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.ReaderWebView;
import com.BibleQuote.BibleQuoteAndroid.utils.ViewUtils;
import com.BibleQuote.BibleQuoteAndroid.R;
import com.BibleQuote.BibleQuoteAndroid.BibleQuoteApp;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncManager;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncOpenChapter;
import com.BibleQuote.BibleQuoteAndroid.async.AsyncSaveChapter;
import com.BibleQuote.bqtj.android.utils.DeviceInfo;
import com.BibleQuote.bqtj.android.utils.DevicesKeyCodes;
import com.BibleQuote.bqtj.entity.BibleReference;
import com.BibleQuote.bqtj.exceptions.BookNotFoundException;
import com.BibleQuote.BibleQuoteAndroid.exceptions.ExceptionHelper;
import com.BibleQuote.bqtj.exceptions.OpenModuleException;
import com.BibleQuote.bqtj.listeners.IReaderViewListener;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.bqtj.managers.bookmarks.BookmarksManager;
import com.BibleQuote.bqtj.modules.Chapter;
import com.BibleQuote.bqtj.utils.*;
import com.BibleQuote.BibleQuoteAndroid.utils.*;
import com.BibleQuote.BibleQuoteAndroid.utils.Share.ShareBuilder.Destination;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.TreeSet;

public class ReaderActivity extends SherlockFragmentActivity implements OnTaskCompleteListener, IReaderViewListener,
		TTSPlayerFragment.onTTSStopSpeakListener {

	private static final String TAG = "ReaderActivity";
	private static final int VIEW_CHAPTER_NAV_LENGHT = 3000;
	private ReaderWebView.Mode oldMode;

	private static final String VIEW_REFERENCE = "com.BibleQuote.intent.action.VIEW_REFERENCE";

	public Librarian getLibrarian() {
		return myLibrarian;
	}

	private Librarian myLibrarian;
	private AsyncManager mAsyncManager;
	private Task mTask;
	private ActionMode currActionMode;

	private String chapterInHTML = "";
	private boolean nightMode = false;
	private String progressMessage = "";

	private TextView vModuleName;
	private TextView vBookLink;
	private LinearLayout btnChapterNav;
	private ReaderWebView vWeb;

	private TTSPlayerFragment ttsPlayer;

	private final int ID_CHOOSE_CH = 1;
	private final int ID_SEARCH = 2;
	private final int ID_HISTORY = 3;
	private final int ID_BOOKMARKS = 4;
	private final int ID_PARALLELS = 5;
	private final int ID_SETTINGS = 6;
	private final int ID_PARTRANSLATES = 7;
	private final int ID_CHECKVERSMAP = 8;
	@Override
	public void onStopSpeak() {
		hideTTSPlayer();
	}

	private final class ActionSelectText implements ActionMode.Callback {

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater infl = getSupportMenuInflater();
			infl.inflate(R.menu.menu_action_text_select, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			TreeSet<Integer> selVerses = vWeb.getSelectedVerses();
			if (selVerses.size() == 0) {
				return true;
			}

			switch (item.getItemId()) {
				case R.id.action_bookmarks:
					myLibrarian.setCurrentVerseNumber(selVerses.first());
					new BookmarksManager(((BibleQuoteApp) getApplication())
							.getCoreContext().getBookmarksRepository())
							.add(myLibrarian.getCurrentOSISLink());
					Toast.makeText(ReaderActivity.this, getString(R.string.added), Toast.LENGTH_LONG).show();
					break;

				case R.id.action_share:
//+					myLibrarian.shareText(ReaderActivity.this, selVerses,
// Destination.ActionSend);
					break;

				case R.id.action_copy:
//+					myLibrarian.shareText(ReaderActivity.this, selVerses,
// Destination.Clipboard);
//+					Toast.makeText(ReaderActivity.this, getString(R.string
// .added), Toast.LENGTH_LONG).show();
					break;

				case R.id.action_references:
					myLibrarian.setCurrentVerseNumber(selVerses.first());
					Intent intParallels = new Intent(VIEW_REFERENCE);
					intParallels.putExtra("linkOSIS", myLibrarian.getCurrentOSISLink().getPath());
					startActivityForResult(intParallels, ID_PARALLELS);
					break;

				case R.id.action_verseeditor:
					vWeb.versesEditor();
					break;

				default:
					return false;
			}

			mode.finish();
			return true;
		}

		public void onDestroyActionMode(ActionMode mode) {
			vWeb.clearSelectedVerse();
			currActionMode = null;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// For javascript debug
		/*final Thread.UncaughtExceptionHandler subclass = Thread.currentThread().getUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
				LogTxt.e(TAG, "uncaughtException", paramThrowable);
				subclass.uncaughtException(paramThread, paramThrowable);
			}
		});*/

		BibleQuoteApp app = (BibleQuoteApp) getApplication();

		boolean isServiceFinish = false;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			isServiceFinish = extras.getBoolean(ServiceActivity.SERVICE_FINISH, false);
		}

		// сервис может успешно завершить работу,
		// но в системе еще виден, как работающий
		if (app.isServiceRunning() && !isServiceFinish) {
			finish();
		} else {

			setContentView(R.layout.reader);

			if ( !DeviceInfo.isEInkSonyPRST()) {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}

			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			getSupportActionBar().setIcon(R.drawable.app_logo);
			getSupportActionBar().setDisplayShowTitleEnabled(false);
			ViewUtils.setActionBarBackground(this);

			myLibrarian = app.getCoreContext().getLibrarian();

			mAsyncManager = app.getAsyncManager();
			mAsyncManager.handleRetainedTask(mTask, this);

			initialyzeViews();
			updateActivityMode();

			BibleReference osisLink = new BibleReference(PreferenceHelper.restoreStateString("last_read"));
			if (!myLibrarian.isOSISLinkValid(osisLink)) {
				onChooseChapterClick();
			} else {
				openChapterFromLink(osisLink);
			}
		}
	}


	private void openChapterFromLink(BibleReference osisLink) {
		mTask = new AsyncOpenChapter(progressMessage, false, myLibrarian, osisLink, null, false);
		mAsyncManager.setupTask(mTask, this);
	}

	private void reloadChapterFromLink(BibleReference osisLink) {
		mTask = new AsyncOpenChapter(progressMessage, false, myLibrarian, osisLink, null, true);
		mAsyncManager.setupTask(mTask, this);
	}

	private void saveChapter(Chapter chapter) {

		// TODO заменить сообщение "Загрузка..." на "Сохранение..."

		mTask = new AsyncSaveChapter(progressMessage, false, myLibrarian, chapter);
		mAsyncManager.setupTask(mTask, this);
	}

	private void openParChapterByModuleID(String ParModuleID) {
		mTask = new AsyncOpenChapter(progressMessage, false, myLibrarian, null, ParModuleID, false);
		mAsyncManager.setupTask(mTask, this);
	}


	private void CheckVersMapByModuleID(String toModuleID) {

		// Всё приложение блокируется ServiceActivity на время выполнения CheckVersificationMap,
		// чтобы контекст приложения (выбранные модули и прочее) не мог поменяться.


		startActivity(new Intent(this, ServiceActivity.class)
				  .putExtra(ServiceActivity.TO_MODULE_ID, toModuleID));

		finish();
	}

	private void SelectParModule() {
		Intent intentParTranslates = new Intent().setClass(getApplicationContext(), LibraryActivity.class);
		intentParTranslates.putExtra("isForParModule", true);
		startActivityForResult(intentParTranslates, ID_PARTRANSLATES);
	}

	private void SelectModuleForCheckVersMap() {
		Intent intentParTranslates = new Intent().setClass(getApplicationContext(), LibraryActivity.class);
		intentParTranslates.putExtra("isForParModule", true);
		startActivityForResult(intentParTranslates, ID_CHECKVERSMAP);
	}

	private void initialyzeViews() {
		btnChapterNav = (LinearLayout) findViewById(R.id.btn_chapter_nav);

		ImageButton btnChapterPrev = (ImageButton) findViewById(R.id.btn_reader_prev);
		btnChapterPrev.setOnClickListener(onClickChapterPrev);
		ImageButton btnChapterNext = (ImageButton) findViewById(R.id.btn_reader_next);
		btnChapterNext.setOnClickListener(onClickChapterNext);

		ImageButton btnChapterUp = (ImageButton) findViewById(R.id.btn_reader_up);
		btnChapterUp.setOnClickListener(onClickPageUp);
		ImageButton btnChapterDown = (ImageButton) findViewById(R.id.btn_reader_down);
		btnChapterDown.setOnClickListener(onClickPageDown);

		vModuleName = (TextView) findViewById(R.id.moduleName);
		vBookLink = (TextView) findViewById(R.id.linkBook);

		progressMessage = getResources().getString(R.string.messageLoad);
		nightMode = PreferenceHelper.restoreStateBoolean("nightMode");

		vWeb = (ReaderWebView) findViewById(R.id.readerView);
		vWeb.setOnReaderViewListener(this);
		vWeb.setMode(PreferenceHelper.isReadModeByDefault() ? ReaderWebView.Mode.Read : ReaderWebView.Mode.Study);
		vWeb.setLibrarian(myLibrarian);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (PreferenceHelper.restoreStateBoolean("DisableAutoScreenRotation")) {
			super.onConfigurationChanged(newConfig);
			this.setRequestedOrientation(Surface.ROTATION_0);
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			super.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater infl = getSupportMenuInflater();
		infl.inflate(R.menu.menu_reader, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		hideTTSPlayer();
		switch (item.getItemId()) {
			case R.id.action_bar_chooseCh:
				onChooseChapterClick();
				break;
			case R.id.action_bar_search:
				Intent intentSearch = new Intent().setClass(getApplicationContext(), SearchActivity.class);
				startActivityForResult(intentSearch, ID_SEARCH);
				break;
			case R.id.action_bar_bookmarks:
				Intent intentBookmarks = new Intent().setClass(getApplicationContext(), BookmarksActivity.class);
				startActivityForResult(intentBookmarks, ID_BOOKMARKS);
				break;
			case R.id.NightDayMode:
				nightMode = !nightMode;
				PreferenceHelper.saveStateBoolean("nightMode", nightMode);
				setTextInWebView();
				break;
			case R.id.action_bar_history:
				Intent intentHistory = new Intent().setClass(getApplicationContext(), HistoryActivity.class);
				startActivityForResult(intentHistory, ID_HISTORY);
				break;
			case R.id.action_speek:
				viewTTSPlayer();
				break;
			case R.id.action_bar_partranslates:
				SelectParModule();
				break;
			case R.id.action_bar_partranslates_switch:
				if (!myLibrarian.isParChapter()) {
					SelectParModule();
				}
				myLibrarian.switchShowParTranslates();
				viewCurrentChapter();
				break;
			case R.id.action_bar_partranslates_checkversmap:
				SelectModuleForCheckVersMap();
				break;
			case R.id.Help:
				Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
				break;
			case R.id.Settings:
				Intent intentSettings = new Intent().setClass(getApplicationContext(), SettingsActivity.class);
				startActivityForResult(intentSettings, ID_SETTINGS);
				break;
			case R.id.About:
				Intent intentAbout = new Intent().setClass(getApplicationContext(), AboutActivity.class);
				startActivity(intentAbout);
				break;
			case R.id.action_verseeditor_save_chapter:
				saveCurrentChapter();
				break;
			case R.id.action_verseeditor_reload_chapter:
				reloadCurrentChapter();
				break;
			default:
				return false;
		}
		return true;
	}

	private void viewTTSPlayer() {
		if (ttsPlayer != null) return;
		ttsPlayer = new TTSPlayerFragment();
		FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
		tran.add(R.id.tts_player_frame, ttsPlayer);
		tran.commit();
		oldMode = vWeb.getMode();
		vWeb.setMode(ReaderWebView.Mode.Speak);
	}

	private void hideTTSPlayer() {
		if (ttsPlayer == null) return;
		FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
		tran.remove(ttsPlayer);
		tran.commit();
		ttsPlayer = null;
		vWeb.setMode(oldMode);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if ((requestCode == ID_BOOKMARKS)
					|| (requestCode == ID_SEARCH)
					|| (requestCode == ID_CHOOSE_CH)
					|| (requestCode == ID_PARALLELS)
					|| (requestCode == ID_HISTORY)) {
				Bundle extras = data.getExtras();
				BibleReference osisLink = new BibleReference(extras.getString("linkOSIS"));
				if (myLibrarian.isOSISLinkValid(osisLink)) {
					openChapterFromLink(osisLink);
                }
			} else if (requestCode == ID_PARTRANSLATES) {
				Bundle extras = data.getExtras();
				BibleReference osisParLink = new BibleReference(extras.getString("linkOSIS"));
				if (myLibrarian.isOSISLinkValid(osisParLink)) {
					openParChapterByModuleID(osisParLink.getModuleID());
				}
			} else if (requestCode == ID_CHECKVERSMAP) {
				Bundle extras = data.getExtras();
				BibleReference osisParLink = new BibleReference(extras.getString("linkOSIS"));
				if (myLibrarian.isOSISLinkValid(osisParLink)) {
					CheckVersMapByModuleID(osisParLink.getModuleID());
				}
			}
		} else if (requestCode == ID_SETTINGS) {
			vWeb.setMode(PreferenceHelper.isReadModeByDefault() ? ReaderWebView.Mode.Read : ReaderWebView.Mode.Study);
			updateActivityMode();
			openChapterFromLink(myLibrarian.getCurrentOSISLink());
		}
	}

	public void setTextInWebView() {
		BibleReference OSISLink = myLibrarian.getCurrentOSISLink();
		vWeb.setText(myLibrarian.getBaseUrl(), chapterInHTML, OSISLink.getFromVerse(), nightMode, myLibrarian.isBible());

		PreferenceHelper.saveStateString("last_read", OSISLink.getExtendedPath());

		vModuleName.setText(myLibrarian.getModuleName());
		vBookLink.setText(myLibrarian.getHumanBookLink());
		btnChapterNav.setVisibility(View.GONE);
	}

	public void onChooseChapterClick() {
		Intent intent = new Intent();
		intent.setClass(this, LibraryActivity.class);
		startActivityForResult(intent, ID_CHOOSE_CH);
	}

	OnClickListener onClickChapterPrev = new OnClickListener() {
		public void onClick(View v) {
			prevChapter();
		}
	};

	OnClickListener onClickChapterNext = new OnClickListener() {
		public void onClick(View v) {
			nextChapter();
		}
	};

	public void prevChapter() {
		try {
			myLibrarian.prevChapter();
		} catch (OpenModuleException e) {
			LogTxt.e(TAG, "prevChapter()", e);
		}
		viewCurrentChapter();
	}

	public void nextChapter() {
		try {
			myLibrarian.nextChapter();
		} catch (OpenModuleException e) {
			LogTxt.e(TAG, "nextChapter()", e);
		}
		viewCurrentChapter();
	}

	OnClickListener onClickPageUp = new OnClickListener() {
		public void onClick(View v) {
			vWeb.pageUp(false);
			viewChapterNav();
		}
	};

	OnClickListener onClickPageDown = new OnClickListener() {
		public void onClick(View v) {
			vWeb.pageDown(false);
			viewChapterNav();
		}
	};

	private void viewCurrentChapter() {
		openChapterFromLink(myLibrarian.getCurrentOSISLink());
	}

	private void reloadCurrentChapter() {
		vWeb.resetFixedVerses();
		reloadChapterFromLink(myLibrarian.getCurrentOSISLink());
	}

	// TODO при переходе на другую главу спросить о сохранении главы
	private void saveCurrentChapter() {
		if (vWeb.isChapterChanged()) {
			vWeb.resetFixedVerses();
			saveChapter(myLibrarian.getCurrChapter());
		}
	}

	public void viewChapterNav() {
		if (chapterNavHandler.hasMessages(R.id.view_chapter_nav)) {
			chapterNavHandler.removeMessages(R.id.view_chapter_nav);
		}

		if (vWeb.getMode() != ReaderWebView.Mode.Study) {
			btnChapterNav.setVisibility(View.GONE);
		} else {
			btnChapterNav.setVisibility(View.VISIBLE);
			if (!vWeb.isScrollToBottom()) {
				Message msg = new Message();
				msg.what = R.id.view_chapter_nav;
				chapterNavHandler.sendMessageDelayed(msg, VIEW_CHAPTER_NAV_LENGHT);
			}
		}
	}

	private Handler chapterNavHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case R.id.view_chapter_nav:
					btnChapterNav.setVisibility(View.GONE);
					break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public boolean onSearchRequested() {
		Intent intentSearch = new Intent().setClass(
				getApplicationContext(), SearchActivity.class);
		startActivityForResult(intentSearch, ID_SEARCH);
		return false;
	}

	public void updateActivityMode() {
		if (vWeb.getMode() == ReaderWebView.Mode.Read) {
			getSupportActionBar().hide();
		} else {
			getSupportActionBar().show();
		}
		viewChapterNav();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP && PreferenceHelper.volumeButtonsToScroll())
				|| DevicesKeyCodes.KeyCodeUp(keyCode)) {
			vWeb.pageUp(false);
			viewChapterNav();
			return true;
		} else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && PreferenceHelper.volumeButtonsToScroll())
				|| DevicesKeyCodes.KeyCodeDown(keyCode)) {
			vWeb.pageDown(false);
			viewChapterNav();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public void onTaskComplete(Task task) {
		if (task != null && !task.isCancelled()) {
			if (task instanceof AsyncOpenChapter) {
				AsyncOpenChapter t = ((AsyncOpenChapter) task);
				if (t.isSuccess()) {
					//chapterInHTML = myLibrarian.getChapterHTMLView();
					chapterInHTML = myLibrarian.getParChapterHTMLView();
					setTextInWebView();
				} else {
					Exception e = t.getException();
					if (e instanceof OpenModuleException) {
						ExceptionHelper.onOpenModuleException((OpenModuleException) e, this, TAG);
					} else if (e instanceof BookNotFoundException) {
						ExceptionHelper.onBookNotFoundException((BookNotFoundException) e, this, TAG);
					}
				}
			}
		}
	}

	@Override
	public void onReaderViewChange(ChangeCode code) {
		if (code == ChangeCode.onChangeReaderMode) {
			updateActivityMode();
		} else if (code == ChangeCode.onUpdateText
				|| code == ChangeCode.onScroll) {
			viewChapterNav();
		} else if (code == ChangeCode.onChangeSelection) {
			TreeSet<Integer> selVerses = vWeb.getSelectedVerses();
			if (selVerses.size() == 0) {
				if (currActionMode != null) {
					currActionMode.finish();
					currActionMode = null;
				}
			} else if (currActionMode == null) {
				currActionMode = startActionMode(new ActionSelectText());
			}
		} else if (code == ChangeCode.onLongPress) {
			viewChapterNav();
			if (vWeb.getMode() == ReaderWebView.Mode.Read) onChooseChapterClick();
		} else if (code == ChangeCode.onDoubleTap) {
			findInDictionaryInternal(vWeb.sWortForDict);
		} else if (code == ChangeCode.onUpNavigation) {
			vWeb.pageUp(false);
		} else if (code == ChangeCode.onDownNavigation) {
			vWeb.pageDown(false);
		} else if (code == ChangeCode.onLeftNavigation) {
			prevChapter();
		} else if (code == ChangeCode.onRightNavigation) {
			nextChapter();
		}
	}

	/*
	private void findInDictionaryInternal(String s) {
		switch (currentDict.internal) {
			case 0:
				Intent intent0 = new Intent(currentDict.action).setComponent(new ComponentName(
						currentDict.packageName, currentDict.className
				)).addFlags(DeviceInfo.getSDKLevel() >= 7 ? FLAG_ACTIVITY_CLEAR_TASK : Intent.FLAG_ACTIVITY_NEW_TASK);
				if (s!=null)
					intent0.putExtra(currentDict.dataKey, s);
				try {
					startActivity( intent0 );
				} catch ( ActivityNotFoundException e ) {
					showToast("Dictionary \"" + currentDict.name + "\" is not installed");
				}
				break;
			case 1:
				final String SEARCH_ACTION  = "colordict.intent.action.SEARCH";
				final String EXTRA_QUERY   = "EXTRA_QUERY";
				final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
				final String EXTRA_HEIGHT  = "EXTRA_HEIGHT";
				final String EXTRA_WIDTH   = "EXTRA_WIDTH";
				final String EXTRA_GRAVITY  = "EXTRA_GRAVITY";
				final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
				final String EXTRA_MARGIN_TOP  = "EXTRA_MARGIN_TOP";
				final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
				final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";

				Intent intent1 = new Intent(SEARCH_ACTION);
				if (s!=null)
					intent1.putExtra(EXTRA_QUERY, s); //Search Query
				intent1.putExtra(EXTRA_FULLSCREEN, true); //
				try
				{
					startActivity(intent1);
				} catch ( ActivityNotFoundException e ) {
					showToast("Dictionary \"" + currentDict.name + "\" is not installed");
				}
				break;
			case 2:
				// Dictan support
				Intent intent2 = new Intent("android.intent.action.VIEW");
				// Add custom category to run the Dictan external dispatcher
				intent2.addCategory("info.softex.dictan.EXTERNAL_DISPATCHER");

				// Don't include the dispatcher in activity
				// because it doesn't have any content view.
				intent2.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

				intent2.putExtra(DICTAN_ARTICLE_WORD, s);

				try {
					startActivityForResult(intent2, DICTAN_ARTICLE_REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					showToast("Dictionary \"" + currentDict.name + "\" is not installed");
				}
				break;
		}
	}
	*/

	private void findInDictionaryInternal(String s) {
		final String SEARCH_ACTION = "colordict.intent.action.SEARCH";
		final String EXTRA_QUERY = "EXTRA_QUERY";
		final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
		final String EXTRA_HEIGHT = "EXTRA_HEIGHT";
		final String EXTRA_WIDTH = "EXTRA_WIDTH";
		final String EXTRA_GRAVITY = "EXTRA_GRAVITY";
		final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
		final String EXTRA_MARGIN_TOP = "EXTRA_MARGIN_TOP";
		final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
		final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";

		Intent intent1 = new Intent(SEARCH_ACTION);
		if (s != null)
			intent1.putExtra(EXTRA_QUERY, s); //Search Query
		intent1.putExtra(EXTRA_FULLSCREEN, true); //
		try {
			startActivity(intent1);
		} catch (ActivityNotFoundException e) {

			//showToast("Dictionary \"" + currentDict.name + "\" is not installed");
			LogTxt.e(TAG, "Dictionary GoldenDict is not installed", e);

		}
	}

}
