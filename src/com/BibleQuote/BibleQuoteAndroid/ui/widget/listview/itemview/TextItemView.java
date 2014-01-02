package com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.itemview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.item.Item;
import com.BibleQuote.BibleQuoteAndroid.ui.widget.listview.item.TextItem;
import com.BibleQuote.BibleQuoteAndroid.R;

public class TextItemView extends LinearLayout implements ItemView {

	private TextView mTextView;

	public TextItemView(Context context) {
		this(context, null);
	}

	public TextItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void prepareItemView() {
		mTextView = (TextView) findViewById(R.id.text);
	}

	public void setObject(Item object) {
		final TextItem item = (TextItem) object;
		mTextView.setText(item.text);
	}
}
