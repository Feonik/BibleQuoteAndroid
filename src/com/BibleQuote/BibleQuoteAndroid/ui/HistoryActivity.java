package com.BibleQuote.BibleQuoteAndroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.BibleQuote.BibleQuoteAndroid.R;
import com.BibleQuote.BibleQuoteAndroid.BibleQuoteApp;
import com.BibleQuote.bqtj.entity.ItemList;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.BibleQuoteAndroid.utils.ViewUtils;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.LinkedList;

public class HistoryActivity extends SherlockFragmentActivity {

	//private final String TAG = "HistoryActivity";

	private ListView vHistoryList;
	private Librarian myLibrarian;
	private LinkedList<ItemList> list = new LinkedList<ItemList>();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorits);
		ViewUtils.setActionBarBackground(this);

		BibleQuoteApp app = (BibleQuoteApp) getApplication();
		myLibrarian = app.getCoreContext().getLibrarian();

		setListAdapter();
		vHistoryList.setOnItemClickListener(OnItemClickListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater infl = getSupportMenuInflater();
		infl.inflate(R.menu.menu_history, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_bar_history_clear:
				myLibrarian.clearHistory();
				setListAdapter();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void setListAdapter() {
		list = myLibrarian.getHistoryList();
		vHistoryList = (ListView) findViewById(R.id.FavoritsLV);
		vHistoryList.setAdapter(new SimpleAdapter(this, list,
				R.layout.item_list_no_id,
				new String[]{ItemList.ID, ItemList.Name}, new int[]{
				R.id.id, R.id.name}));
	}

	private AdapterView.OnItemClickListener OnItemClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			Intent intent = new Intent();
			intent.putExtra("linkOSIS", list.get(position).get(ItemList.ID));
			setResult(RESULT_OK, intent);
			finish();
		}
	};

}
