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
package com.BibleQuote.BibleQuoteAndroid.ui.widget;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import com.BibleQuote.bqtj.utils.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import com.BibleQuote.BibleQuoteAndroid.R;
import com.BibleQuote.bqtj.listeners.IReaderViewListener;
import com.BibleQuote.bqtj.listeners.IReaderViewListener.ChangeCode;
import com.BibleQuote.bqtj.managers.Librarian;
import com.BibleQuote.bqtj.android.utils.DeviceInfo;
import com.BibleQuote.bqtj.utils.PreferenceHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TreeSet;

public class ReaderWebView extends WebView
		implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	final String TAG = "ReaderWebView";

	private GestureDetector mGestureScanner;
	private JavaScriptInterface jsInterface;

	protected TreeSet<Integer> selectedVerse = new TreeSet<Integer>();

	public TreeSet<Integer> getSelectedVerses() {
		return this.selectedVerse;
	}

	public void setSelectedVerse(TreeSet<Integer> selectedVerse) {
		jsInterface.clearSelectedVerse();
		this.selectedVerse = selectedVerse;
		for (Integer verse : selectedVerse) {
			jsInterface.setSelectedVerse(verse);
		}
	}

	public void gotoVerse(int verse) {
		jsInterface.gotoVerse(verse);
	}

	public void versesEditor() {
		for (Integer verse : selectedVerse) {
			jsInterface.verseEditor(verse);
		}
		selectedVerse.clear();
	}

	private boolean isChapterChanged = false;

	public boolean isChapterChanged() {
		return isChapterChanged;
	}

	public void resetFixedVerses() {
		isChapterChanged = false;
	}

	public static enum Mode {
		Read, Study, Speak
	}

	private Mode currMode = Mode.Read;

	public void setMode(Mode mode) {
		currMode = mode;
		if (currMode != Mode.Study) {
			clearSelectedVerse();
		}
		notifyListeners(ChangeCode.onChangeReaderMode);
	}

	public Mode getMode() {
		return currMode;
	}


	protected Librarian myLibrarian;

	public void setLibrarian(Librarian librarian) {
		myLibrarian = librarian;
	}

	private ArrayList<IReaderViewListener> listeners = new ArrayList<IReaderViewListener>();

	public void setOnReaderViewListener(IReaderViewListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	private void notifyListeners(ChangeCode code) {
		for (IReaderViewListener listener : listeners) {
			listener.onReaderViewChange(code);
		}
	}

	public boolean mPageLoaded = false;

	public ReaderWebView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		WebSettings settings = getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setNeedInitialFocus(false);
		settings.setBuiltInZoomControls(false);
		settings.setSupportZoom(false);

		setFocusable(true);
		setFocusableInTouchMode(true);
		setWebViewClient(new webClient());
		setWebChromeClient(new chromeClient());

		// <-- bug -- для вывода виртуальной клавиатуры
		// http://code.google.com/p/android/issues/detail?id=7189
		// Issue 7189: 	WebView with input/textarea never get virtual keyboard focus
		setHapticFeedbackEnabled(true);
		setClickable(true);
		requestFocus(View.FOCUS_DOWN);
		//settings.setUseWideViewPort(true); //-- делает слишком широкой <textarea>

		setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_UP:
						if (!v.hasFocus()) {
							v.requestFocus();
						}
						break;
				}
				return false;
			}
		});
		// -->

		this.jsInterface = new JavaScriptInterface();
		addJavascriptInterface(this.jsInterface, "reader");

		setVerticalScrollbarOverlay(true);

		mGestureScanner = new GestureDetector(context, this);
		mGestureScanner.setIsLongpressEnabled(true);
		mGestureScanner.setOnDoubleTapListener(this);
	}

	public void setText(String baseUrl, String text, int currVerse, Boolean nightMode, Boolean isBible) {
		mPageLoaded = false;
		String modStyle = isBible ? "bible_style.css" : "book_style.css";

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n");
		html.append("<html>\r\n");
		html.append("<head>\r\n");
		html.append("<meta http-equiv=Content-Type content=\"text/html; charset=UTF-8\">\r\n");
		html.append("<script language=\"JavaScript\" src=\"file:///android_asset/reader.js\" type=\"text/javascript\"></script>\r\n");
		html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/reader.css\">\r\n");
		html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/").append(modStyle).append("\">\r\n");
		html.append(getStyle(nightMode));
		html.append("</head>\r\n");
		html.append("<body").append(currVerse > 1 ? (" onLoad=\"document.location.href='#verse_" + currVerse + "';\"") : "").append(">\r\n");
		html.append(text);
		html.append("</body>\r\n");
		html.append("</html>");

		loadDataWithBaseURL("file://" + baseUrl, html.toString(), "text/html", "UTF-8", "about:config");
		jsInterface.clearSelectedVerse();
	}

	public boolean isScrollToBottom() {
		int scrollY = getScrollY();
		int scrollExtent = computeVerticalScrollExtent();
		int scrollPos = scrollY + scrollExtent;
		return (scrollPos >= (computeVerticalScrollRange() - 10));
	}

    private int mUpdateMode = 34;

    /*
    *  Переключение режима отображения екрана для
    *  Sony PRS-T2/T1
    *  Взято из NoRefreshEnabler (http://www.mobileread.com/forums/showpost.php?p=1956700&postcount=33)
    *  mUpdateMode = 34  -- нормальный режим, 16 оттенков серого
    *  mUpdateMode = 5   -- чернобелый режим, 2 цвета
    * */
    @Override
    public void invalidate() {
        //super.invalidate(mUpdateMode);
        if (DeviceInfo.isEInkSonyPRST()) {
            try {
                Method invalidateMethod = super.getClass().getMethod("invalidate", int.class);
                invalidateMethod.invoke(this, mUpdateMode);

                View vRootV = getRootView();
                if (vRootV != this) {
                    vRootV.invalidate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            super.invalidate();
        }
    }

    private long lKeyDownTime = 0;

    /*
    * При нажатии на кнопку переключаем на NoRefresh
    * */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DeviceInfo.isEInkSonyPRST()) {
            mUpdateMode = 5;
            invalidate();
            lKeyDownTime = System.currentTimeMillis();
        }
        return false;
    }

    final static int iKeyTimeDelay = 2000;
    /*
    * При отпускании кнопки возвращаем режим Refresh,
    * если только снова не нажали кнопку за время меньше задержки.
    * */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (DeviceInfo.isEInkSonyPRST()) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    if ((System.currentTimeMillis() - lKeyDownTime) > iKeyTimeDelay) {
                        mUpdateMode = 34;
                        invalidate();
                    }
                }
            }, iKeyTimeDelay);
        }

        return false;
    }

    public void computeScroll() {
		super.computeScroll();
		if (mPageLoaded && isScrollToBottom()) {
			notifyListeners(ChangeCode.onScroll);
		}
	}

	public void clearSelectedVerse() {
		if (selectedVerse.size() == 0) {
			return;
		}
		jsInterface.clearSelectedVerse();
		if (currMode == Mode.Study) {
			notifyListeners(ChangeCode.onChangeSelection);
		}
	}

	private String getStyle(Boolean nightMode) {
		String textColor;
		String backColor;
		String selTextColor;
		String selTextBack;

		getSettings().setStandardFontFamily(PreferenceHelper.getFontFamily());

		if (!nightMode) {
			backColor = PreferenceHelper.getTextBackground();
			textColor = PreferenceHelper.getTextColor();
//			selTextColor = "#FEF8C4";
			selTextColor = PreferenceHelper.getTextColorSelected();
			selTextBack = PreferenceHelper.getTextBackgroundSelected();

		} else {
			textColor = "#EEEEEE";
			backColor = "#000000";
			selTextColor = "#EEEEEE";
			selTextBack = "#562000";
		}
		String textSize = PreferenceHelper.getTextSize();

		StringBuilder style = new StringBuilder();
		style.append("<style type=\"text/css\">\r\n");
		style.append("body {\r\n");
		style.append("padding-bottom: 50px;\r\n");
		if (PreferenceHelper.textAlignJustify()) {
			style.append("text-align: justify;\r\n");
		}
		//style.append("font-family: Georgia, Tahoma, Verdana, sans-serif;\r\n");
		style.append("color: ").append(textColor).append(";\r\n");
		style.append("font-size: ").append(textSize).append("pt;\r\n");
		style.append("line-height: 1.25;\r\n");
		style.append("background: ").append(backColor).append(";\r\n");
		style.append("}\r\n");
		style.append(".verse {\r\n");
		style.append("background: ").append(backColor).append(";\r\n");
		style.append("}\r\n");
		style.append(".selectedVerse {\r\n");
		style.append("color: ").append(selTextColor).append(";\r\n");
		style.append("background: ").append(selTextBack).append(";\r\n");
		style.append("}\r\n");
		style.append("img {\r\n");
		style.append("max-width: 100%;\r\n");
		style.append("}\r\n");
		style.append("</style>\r\n");

		return style.toString();
	}

	final class webClient extends WebViewClient {
		webClient() {
		}

		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i(TAG, "shouldOverrideUrlLoading(" + url + ")");
			return true;
		}

		public void onPageFinished(WebView paramWebView, String paramString) {
			super.onPageFinished(paramWebView, paramString);
			mPageLoaded = true;
		}
	}

/*
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureScanner.onTouchEvent(event) || (event != null && super.onTouchEvent(event));
	}
*/

    /*
    * Детектируем отпускание экрана
    * */
    public boolean onTouchEvent(MotionEvent event) {
        if (DeviceInfo.isEInkSonyPRST()) {
            boolean detectedUp = (event.getAction() == MotionEvent.ACTION_UP);
            boolean isUp = false;
            if (!mGestureScanner.onTouchEvent(event) && detectedUp) {
                isUp = onUp(event);
            }
            return isUp || (event != null && super.onTouchEvent(event));
        } else {
            return mGestureScanner.onTouchEvent(event) || (event != null && super.onTouchEvent(event));
        }
    }

	public boolean onSingleTapUp(MotionEvent event) {
		return false;
	}

	public boolean onSingleTapConfirmed(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		if (currMode == Mode.Study) {
			if (Build.VERSION.SDK_INT < 8) {
				y += getScrollY();
			}
			float density = getContext().getResources().getDisplayMetrics().density;
			final int x_final = (int) (x / density);
			final int y_final = (int) (y / density);

			post(new Runnable() {
				@Override
				public void run() {
					loadUrl("javascript:handleClick(" + x_final + ", " + y_final + ");");
				}
			});

			notifyListeners(ChangeCode.onChangeSelection);
		} else if (currMode == Mode.Read) {
			int width = this.getWidth();
			int height = this.getHeight();

			if (((float) y / height) <= 0.33) {
				notifyListeners(ChangeCode.onUpNavigation);
			} else if (((float) y / height) > 0.67) {
				notifyListeners(ChangeCode.onDownNavigation);
			} else if (((float) x / width) <= 0.33) {
				notifyListeners(ChangeCode.onLeftNavigation);
			} else if (((float) x / width) > 0.67) {
				notifyListeners(ChangeCode.onRightNavigation);
			}
		}
		return false;
	}

    private long lDownTime = 0;

    /*
    * При нажатии на кнопку переключаем на NoRefresh
    * */
    public boolean onDown(MotionEvent event) {
        if (DeviceInfo.isEInkSonyPRST()) {
            mUpdateMode = 5;
            invalidate();
            lDownTime = System.currentTimeMillis();
        }
		return false;
	}

    final static int iTimeDelay = 2000;

    /*
    * При отпускании экрана возвращаем режим Refresh,
    * если только снова не коснулись экрана за время меньше задержки.
    * */
    public boolean onUp(MotionEvent event) {

        if (DeviceInfo.isEInkSonyPRST()) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    if ((System.currentTimeMillis() - lDownTime) > iTimeDelay) {
                        mUpdateMode = 34;
                        invalidate();
                    }
                }
            }, iTimeDelay);
        }

        return false;
    }

	public boolean onFling(MotionEvent e1, MotionEvent e2,
						   float velocityX, float velocityY) {
		notifyListeners(ChangeCode.onScroll);
		return false;
	}

	public void onLongPress(MotionEvent event) {
		// вынужденная мера, после DoubleTap при страрте стороннего приложения
		// почему-то срабатывает иногда и LongPress
		if (event.getEventTime() - lDblTapTime < 400) {
			return;
		}

		notifyListeners(ChangeCode.onLongPress);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
		notifyListeners(ChangeCode.onScroll);
		return false;
	}

	public void onShowPress(MotionEvent event) {
	}

	long lDblTapTime = 0;

	public boolean onDoubleTap(MotionEvent event) {
		// вынужденная мера, после DoubleTap при страрте стороннего приложения
		// почему-то срабатывает иногда и LongPress
		lDblTapTime = event.getEventTime();

		if (currMode != Mode.Speak) {
			//setMode(currMode == Mode.Study ? Mode.Read : Mode.Study);

			int x = (int) event.getX();
			int y = (int) event.getY();
//			if (currMode == Mode.Study) {
				if (Build.VERSION.SDK_INT < 8) {
					y += getScrollY();
				}
				float density = getContext().getResources().getDisplayMetrics().density;
				x = (int) (x / density);
				y = (int) (y / density);


			if (DeviceInfo.isEInkSonyPRST()) {

				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {

						if ((System.currentTimeMillis() - lDownTime) > 10) {
							mUpdateMode = 34;
							invalidate();
						}
					}
				}, 10);
			}

			loadUrl("javascript:dblClickWord(" + x + ", " + y + ");");
//			} else if (currMode == Mode.Read) {

		}

		return false;
	}

	public boolean onDoubleTapEvent(MotionEvent event) {
		return false;
	}

    /*
    * При касании экрана режим был переключен на NoRefresh,
    * если после было событие onLongPress, то активность уже потеряла управление --
    * перерисовываем экран при появлении активности.
    * (Оказалось очень удобно в режиме чтения -- при выборе книг/глав из списка
    * включен режим NoRefresh, соответственно быстро работает скролл
    * и переключение по книгам/главам.)
    * */
    protected void onWindowVisibilityChanged(int visibility) {
        if (DeviceInfo.isEInkSonyPRST()) {
            if (visibility == View.VISIBLE) {

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mUpdateMode = 34;
                        invalidate();
                    }
                }, 1000);

            }
        }
    }

	final class chromeClient extends WebChromeClient {
		chromeClient() {
		}

		public boolean onJsAlert(WebView webView, String url, String message, JsResult result) {
			Log.i(TAG, message);
			if (result != null)
				result.confirm();
			return true;
		}
	}

	public String sWortForDict = "";

	final class JavaScriptInterface {

		public JavaScriptInterface() {
			clearSelectedVerse();
		}

		public void clearSelectedVerse() {
			for (final Integer verse : selectedVerse) {

				post(new Runnable() {
					@Override
					public void run() {
						loadUrl("javascript: deselectVerse('verse_" + verse + "');");
					}
				});

			}
			selectedVerse.clear();
		}

		public void onClickVerse(String id) {
			if (currMode != Mode.Study || !id.contains("verse")) {
				return;
			}

			final Integer verse;
			try{
				verse = Integer.parseInt(id.split("_")[1]);
			} catch (NumberFormatException e) {
				return;
			}

			if (selectedVerse.contains(verse)) {
				selectedVerse.remove(verse);

				post(new Runnable() {
					@Override
					public void run() {
						loadUrl("javascript: deselectVerse('verse_" + verse + "');");
					}
				});

			} else {
				selectedVerse.add(verse);
				setSelectedVerse(verse);
			}

			try {
				Handler mHandler = getHandler();
				mHandler.post(new Runnable() {
					public void run() {
						notifyListeners(ChangeCode.onChangeSelection);
					}
				});
			} catch (NullPointerException e) {
				Log.e(TAG, "Error when notifying clients ReaderWebView");
			}
		}

		private void setSelectedVerse(final int verse) {

			post(new Runnable() {
				@Override
				public void run() {
					loadUrl("javascript: selectVerse('verse_" + verse + "');");
				}
			});

		}

		public void gotoVerse(final int verse) {

			post(new Runnable() {
				@Override
				public void run() {
					loadUrl("javascript: gotoVerse(" + verse + ");");
				}
			});

		}

		public void alert(final String message) {
			Log.i(TAG, "JavaScriptInterface.alert()");
		}

		public void verseEditor(final int iVerseNumber) {

			if (selectedVerse.contains(iVerseNumber)) {

				String sVerseText = myLibrarian.getCurrChapter().getVerse(iVerseNumber).getText();

				sVerseText = sVerseText.replace(String.format("%n"), "\\n");

				sVerseText = "<form><textarea id='v_editor_"
						  + iVerseNumber
						  + "' class='verse_editor' wrap='soft' rows='3'>"
						  + sVerseText
						  + "</textarea><button onclick=\"fixVerse('"
						  + iVerseNumber
						  + "')\">"
						  + getContext().getResources().getString(R.string.verseeditor_fix_verse)
						  + "</button><button onclick=\"cancelEdit('"
						  + iVerseNumber + "')\">"
						  + getContext().getResources().getString(R.string.verseeditor_cancel_edit_verse)
						  + "</button></form>";

				sVerseText = sVerseText.replaceAll("([^\\\\])(')", "$1\\\\$2");


				final String sVerseText_final = sVerseText;

				post(new Runnable() {
					@Override
					public void run() {
						loadUrl("javascript: setVerseForEdit('" + iVerseNumber + "', '" + sVerseText_final + "');");
					}
				});

			}
		}

		public void fixVerse(final String sVerseNum, String sVerseText) {
			Integer iVerseNumber = Integer.parseInt(sVerseNum);

			sVerseText = sVerseText.replace("\n", String.format("%n"));

			myLibrarian.fixVerseText(myLibrarian.getCurrChapter(), iVerseNumber, sVerseText);
			isChapterChanged = true;

			sVerseText = myLibrarian.getVerseTextHtmlBody(myLibrarian.getCurrModule(), sVerseText)
					  .replaceAll("([^\\\\])(')", "$1\\\\$2")
					  .replace(String.format("%n"), "\\n");

			final String sVerseText_final = sVerseText;

			post(new Runnable() {
				@Override
				public void run() {
					loadUrl("javascript: setVerseAfterEdit('" + sVerseNum + "', '" + sVerseText_final + "');");
				}
			});

		}

		public void cancelEdit(final String sVerseNum) {
			Integer iVerseNumber = Integer.parseInt(sVerseNum);

			String sVerseText = myLibrarian.getCurrChapter().getVerse(iVerseNumber).getText();

//			if (myLibrarian.isShowParTranslates) {
//				sVerseText = myLibrarian.getVerseTextHtmlBodyForParTr(myLibrarian.getCurrModule(), sVerseText)
//						  .replaceAll("([^\\\\])(')", "$1\\\\$2")
//						  .replace(String.format("%n"), "\\n");
//
//			} else {
				sVerseText = myLibrarian.getVerseTextHtmlBody(myLibrarian.getCurrModule(), sVerseText)
						  .replaceAll("([^\\\\])(')", "$1\\\\$2")
						  .replace(String.format("%n"), "\\n");
//			}



			final String sVerseText_final = sVerseText;

			post(new Runnable() {
				@Override
				public void run() {
					loadUrl("javascript: setVerseAfterEdit('" + sVerseNum + "', '" + sVerseText_final + "');");
				}
			});

		}

		public void onDoubleClickOfWord(String SpanWord) {
			sWortForDict = SpanWord;
			notifyListeners(ChangeCode.onDoubleTap);
		}


	}
}
