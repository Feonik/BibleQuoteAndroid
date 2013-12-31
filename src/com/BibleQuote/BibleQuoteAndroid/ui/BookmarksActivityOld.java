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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.BibleQuote.bqtj.utils.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.ItemAdapter;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.item.BookmarkItem;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.item.Item;
import com.BibleQuote.R;
import com.BibleQuote.BibleQuoteAndroid.BibleQuoteApp;
import com.BibleQuote.bqtj.entity.BibleReference;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.bqtj.managers.bookmarks.Bookmark;
import com.BibleQuote.bqtj.managers.bookmarks.BookmarksManager;
import com.BibleQuote.BibleQuoteAndroid.utils.ViewUtils;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class BookmarksActivityOld extends SherlockFragmentActivity {

	private final String TAG = "BookmarksActivityOld";

	private ListView LV;
	private Librarian myLibrarian;
	private Bookmark currBookmark;
	private BookmarksManager bookmarksManager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorits);
		ViewUtils.setActionBarBackground(this);

		BibleQuoteApp app = (BibleQuoteApp) getApplication();
		myLibrarian = app.getCoreContext().getLibrarian();

		bookmarksManager = new BookmarksManager(
				app.getCoreContext().getBookmarksRepository());

		LV = (ListView) findViewById(R.id.FavoritsLV);
		LV.setOnItemClickListener(OnItemClickListener);
		LV.setOnItemLongClickListener(OnItemLongClickListener);
		setAdapter();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater infl = getSupportMenuInflater();
		infl.inflate(R.menu.menu_bookmarks, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_bar_sort:
				bookmarksManager.sort();
				setAdapter();
				break;

			case R.id.action_bar_delete:
				Builder builder = new AlertDialog.Builder(BookmarksActivityOld.this);
				builder.setIcon(R.drawable.icon);
				builder.setTitle(R.string.bookmarks);
				builder.setMessage(R.string.fav_delete_all_question);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						bookmarksManager.deleteAll();
						setAdapter();
					}
				});
				builder.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.show();
				break;

			default:
				break;
		}
		return true;
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		setAdapter();
	}

	private AdapterView.OnItemClickListener OnItemClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			currBookmark = ((BookmarkItem) LV.getAdapter().getItem(position)).bookmark;
			Log.i(TAG, "Select bookmark: " + currBookmark.humanLink + " (OSIS link = " + currBookmark.OSISLink + ")");

			BibleReference osisLink = new BibleReference(currBookmark.OSISLink);
			if (!myLibrarian.isOSISLinkValid(osisLink)) {
				Log.i(TAG, "Delete invalid bookmark: " + currBookmark);
				bookmarksManager.delete(currBookmark);
				setAdapter();
				Toast.makeText(getApplicationContext(), R.string.bookmark_invalid_removed, Toast.LENGTH_LONG).show();
			} else {
				Intent intent = new Intent();
				intent.putExtra("linkOSIS", currBookmark.OSISLink);
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	};

	private AdapterView.OnItemLongClickListener OnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
			currBookmark = ((BookmarkItem) LV.getAdapter().getItem(position)).bookmark;
			Builder b = new AlertDialog.Builder(BookmarksActivityOld.this);
			b.setIcon(R.drawable.icon);
			b.setTitle(currBookmark.humanLink);
			b.setMessage(R.string.fav_question_del_fav);
			b.setPositiveButton("OK", positiveButton_OnClick);
			b.setNegativeButton(R.string.cancel, null);
			b.show();
			return true;
		}
	};

	private DialogInterface.OnClickListener positiveButton_OnClick = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			Log.i(TAG, "Delete bookmark: " + currBookmark);
			bookmarksManager.delete(currBookmark);
			setAdapter();
			Toast.makeText(getApplicationContext(), R.string.removed, Toast.LENGTH_LONG).show();
		}
	};

	private void setAdapter() {
		List<Item> items = new ArrayList<Item>();
		for (Bookmark curr : bookmarksManager.getAll()) {
			items.add(new BookmarkItem(curr));
		}
		ItemAdapter adapter = new ItemAdapter(this, items);
		LV.setAdapter(adapter);
	}
}
