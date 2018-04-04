package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import io.auraapp.auraandroid.common.EmojiHelper;

public class ScreenFragment extends Fragment {
    /**
     * Context is provided and getContext is overridden because children might use
     * the activity's context before the fragment's view has been created.
     */
    private Context mContext;

    @Override
    @NonNull
    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    protected void toast(@StringRes int text) {
        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }
}
