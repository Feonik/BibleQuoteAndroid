package com.BibleQuote.BibleQuoteAndroid.async;

import com.BibleQuote.bqtj.utils.Log;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.bqtj.modules.Chapter;
import com.BibleQuote.BibleQuoteAndroid.utils.Task;

public class AsyncSaveChapter extends Task {
	private final String TAG = "AsyncOpenChapter";

	private Librarian librarian;
	private Chapter chapter;
	private Exception exception;
	private Boolean isSuccess;

	public AsyncSaveChapter(String message, Boolean isHidden, Librarian librarian, Chapter chapter) {
		super(message, isHidden);
		this.librarian = librarian;
		this.chapter = chapter;
	}

	@Override
	protected Boolean doInBackground(String... arg0) {
		isSuccess = false;
//		try {
			Log.i(TAG, String.format("Save chapter: moduleID=%1$s, bookID=%2$s, chapterNumber=%3$s",
					chapter.getBook().getModule().getID(), chapter.getBook().getID(), chapter.getNumber().toString()));

			// TODO создать и обработать исключения
			librarian.saveChapter(chapter);
			isSuccess = true;

//		} catch (OpenModuleException e) {
//			exception = e;
//		} catch (BookNotFoundException e) {
//			exception = e;
//		}
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
