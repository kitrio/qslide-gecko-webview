/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

public class LocationView extends AppCompatEditText {

    private CommitListener commitListener;
    private FocusAndCommitListener focusCommitListener = new FocusAndCommitListener();

    public interface CommitListener {
        void onCommit(String text);
    }

    public LocationView(Context context) {
        super(context);

        setOnFocusChangeListener(focusCommitListener);
        setOnEditorActionListener(focusCommitListener);
    }
    public LocationView(Context context, AttributeSet attrs) {
        super(context ,attrs);

        setOnFocusChangeListener(focusCommitListener);
        setOnEditorActionListener(focusCommitListener);
    }

    public void setCommitListener(CommitListener listener) {
        commitListener = listener;
    }

    private class FocusAndCommitListener implements OnFocusChangeListener, OnEditorActionListener {
        private String initialText;
        private boolean committed;

        @Override
        public void onFocusChange(View view, boolean focused) {
            if (focused) {
                initialText = ((TextView)view).getText().toString();
                committed = false;
            } else if (!committed) {
                setText(initialText);
            }
        }

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (commitListener != null) {
                commitListener.onCommit(textView.getText().toString());
            }

            committed = true;
            return true;
        }
    }
}
