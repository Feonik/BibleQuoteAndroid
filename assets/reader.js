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

function selectVerse(id) {
	element = document.getElementById(id);
	element.className = "selectedVerse";
}

function deselectVerse(id) {
	element = document.getElementById(id);
	element.className = "verse";
}

function handleClick(x, y) {
	var element = document.elementFromPoint(x, y);

	while (element != null && element.id.indexOf('verse') == -1) {
		if (element instanceof window.HTMLAnchorElement && (element.href.indexOf('#') != -1)) {
			return;
		}
		element = element.parentElement;
	}
	if (element != null) {
		reader.onClickVerse(element.id);	
	}
}

function gotoVerse(number) {
    document.location.href='#verse_' + number;
}

function dblClickWord(x, y) {
	var element = document.elementFromPoint(x, y);

	while ((element != null) && (element.tagName.indexOf('SPAN') == -1)) {
//		if (element instanceof window.HTMLAnchorElement && (element.href.indexOf('#') != -1)) {
//			return;
//		}
		element = element.parentElement;
	}

	if (element != null) {
		reader.onDoubleClickOfWord(element.innerHTML);
	}
}

function fixVerse(verseNum) {
	var verse_editor = document.getElementById('v_editor_' + verseNum);
	reader.fixVerse(verseNum, verse_editor.value);
}

function cancelEdit(verseNum) {
	reader.cancelEdit(verseNum);
}

function setVerseForEdit(verseNum, VerseText) {
	var verse = document.getElementById('verse_' + verseNum);
	verse.id = 'editver_' + verseNum;
    verse.className = "verse";
	verse.innerHTML = VerseText;
	//console.error(document.documentElement.innerHTML);
}

function setVerseAfterEdit(verseNum, VerseText) {
	var verse = document.getElementById('editver_' + verseNum);
    verse.id = 'verse_' + verseNum;
	verse.innerHTML = VerseText;
}
