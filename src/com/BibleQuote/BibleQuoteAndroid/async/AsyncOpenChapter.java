package com.BibleQuote.BibleQuoteAndroid.async;

import com.BibleQuote.bqtj.utils.Log;
import com.BibleQuote.bqtj.entity.BibleReference;
import com.BibleQuote.bqtj.exceptions.BookNotFoundException;
import com.BibleQuote.bqtj.exceptions.OpenModuleException;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.BibleQuoteAndroid.utils.Task;

public class AsyncOpenChapter extends Task {
	private final String TAG = "AsyncOpenChapter";

	private Librarian librarian;
	private BibleReference link;
	private String ParModuleID;
	private Exception exception;
	private Boolean isSuccess;
	private Boolean isReload;

	public AsyncOpenChapter(String message, Boolean isHidden, Librarian librarian, BibleReference link,
							String ParModuleID, Boolean isReload) {
		super(message, isHidden);
		this.librarian = librarian;
		this.link = link;
		this.ParModuleID = ParModuleID;
		this.isReload = isReload;
	}

	@Override
	protected Boolean doInBackground(String... arg0) {
		isSuccess = false;
		try {
			if (link != null) {
				Log.i(TAG, String.format("Open OSIS link with moduleID=%1$s, bookID=%2$s, chapterNumber=%3$s, verseNumber=%4$s",
						link.getModuleID(), link.getBookID(), link.getChapter(), link.getFromVerse()));

				librarian.openChapter(link, isReload);
			} else if (ParModuleID != null) {
				Log.i(TAG, String.format("Open ParChapter by moduleID=%1$s", ParModuleID));

				librarian.openParChapter(ParModuleID);
			}

			isSuccess = true;

		} catch (OpenModuleException e) {
			exception = e;
		} catch (BookNotFoundException e) {
			exception = e;
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
	}

	public Exception getException() {
		return exception;
	}

	public Boolean isSuccess() {
		return isSuccess;
	}
}
