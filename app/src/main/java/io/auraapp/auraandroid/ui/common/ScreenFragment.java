package io.auraapp.auraandroid.ui.common;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import io.auraapp.auraandroid.common.EmojiHelper;

public class ScreenFragment extends Fragment {

    protected void toast(@StringRes int text) {
        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }
}
