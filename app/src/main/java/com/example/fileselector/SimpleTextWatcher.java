package com.example.fileselector;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {
    private final OnTextChanged callback;

    public interface OnTextChanged { void onTextChanged(String text); }

    public SimpleTextWatcher(OnTextChanged callback) {
        this.callback = callback;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        callback.onTextChanged(s.toString());
    }
    @Override public void afterTextChanged(Editable s) {}
}
