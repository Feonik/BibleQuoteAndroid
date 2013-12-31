package com.BibleQuote.BibleQuoteAndroid.utils.Share;

import android.content.Context;
import com.BibleQuote.bqtj.modules.Book;
import com.BibleQuote.bqtj.modules.Chapter;
import com.BibleQuote.bqtj.modules.Module;
import com.BibleQuote.bqtj.utils.BibleReferenceFormatter.EmptyReferenceFormatter;
import com.BibleQuote.bqtj.utils.BibleReferenceFormatter.FullReferenceFormatter;
import com.BibleQuote.bqtj.utils.BibleReferenceFormatter.IBibleReferenceFormatter;
import com.BibleQuote.bqtj.utils.BibleReferenceFormatter.ShortReferenceFormatter;
import com.BibleQuote.bqtj.utils.BibleTextFormatters.BreakVerseFormatter;
import com.BibleQuote.bqtj.utils.BibleTextFormatters.IBibleTextFormatter;
import com.BibleQuote.bqtj.utils.BibleTextFormatters.SimpleFormatter;
import com.BibleQuote.bqtj.utils.PreferenceHelper;

import java.util.LinkedHashMap;
import java.util.TreeSet;

public abstract class BaseShareBuilder {
	IBibleTextFormatter textFormater;
	IBibleReferenceFormatter referenceFormatter;

	Context context;
	Module module;
	Book book;
	Chapter chapter;
	LinkedHashMap<Integer, String> verses;

	protected void InitFormatters() {
		boolean breakVerse = PreferenceHelper.divideTheVerses();
		if (breakVerse) {
			textFormater = new BreakVerseFormatter(verses);
		} else {
			textFormater = new SimpleFormatter(verses);
		}

		TreeSet<Integer> verseNumbers = new TreeSet<Integer>();
		for (Integer numb : verses.keySet()) {
			verseNumbers.add(numb);
		}

		String chapterNumber = String.valueOf(chapter.getNumber());

		boolean addLink = PreferenceHelper.addReference();
		boolean shortLink = PreferenceHelper.shortReference();
		if (!addLink) {
			referenceFormatter = new EmptyReferenceFormatter(module, book, chapterNumber, verseNumbers);
		} else if (shortLink) {
			referenceFormatter = new ShortReferenceFormatter(module, book, chapterNumber, verseNumbers);
		} else {
			referenceFormatter = new FullReferenceFormatter(module, book, chapterNumber, verseNumbers);
		}
	}

	protected String getShareText() {
		String text = textFormater.format();
		if (!PreferenceHelper.addReference()) {
			return text;
		}

		String reference = referenceFormatter.getLink();
		if (PreferenceHelper.putReferenceInBeginning()) {
			return String.format("%1$s - %2$s", reference, text);
		} else {
			return String.format("%1$s (%2$s)", text, reference);
		}
	}

	public abstract void share();
}
