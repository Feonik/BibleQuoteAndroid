/*
 * Copyright (C) 2011 Scripture Software (http://scripturesoftware.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.BibleQuote.BibleQuoteAndroid.ui.fragments;

import android.os.Bundle;
import com.BibleQuote.bqtj.utils.Log;
import android.view.View;
import android.widget.ListView;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.ItemAdapter;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.item.Item;
import com.BibleQuote.R;
import com.BibleQuote.BibleQuoteAndroid.BibleQuoteApp;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.bqtj.managers.bookmarks.Bookmark;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Vladimir Yakushev
 * Date: 14.05.13
 */
public class TagsFragment extends SherlockListFragment {
	private final static String TAG = TagsFragment.class.getSimpleName();

	private Librarian myLibrarian;
	private Bookmark currBookmark;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		setEmptyText(getResources().getText(R.string.empty));

		BibleQuoteApp app = (BibleQuoteApp) getSherlockActivity().getApplication();
		myLibrarian = app.getCoreContext().getLibrarian();

		ListView lw = getListView();
		lw.setLongClickable(true);
		lw.setOnLongClickListener(OnItemLongClickListener);

		setAdapter();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(TAG, "onCreateOptionsMenu");
		//inflater.inflate(R.menu.menu_bookmarks, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView LV, View v, int position, long id) {
	}

	private View.OnLongClickListener OnItemLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View view) {
			return true;
		}
	};

	private void setAdapter() {
		List<Item> items = new ArrayList<Item>();
		ItemAdapter adapter = new ItemAdapter(getSherlockActivity(), items);
		setListAdapter(adapter);
	}
}
