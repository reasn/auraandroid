package io.auraapp.auraandroid.ui.common.fragments;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.MainActivity;

import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.v;

abstract public class ContextFragment extends Fragment {

    private static final String TAG = "@aura/ui/common/" + ContextFragment.class.getSimpleName();

    abstract protected void onResumeWithContext(MainActivity activity);

    protected void onPauseWithContext(MainActivity activity) {
    }

    private MainActivity getMainActivity() {
        Context context = getContext();
        if (context != null && !(context instanceof MainActivity)) {
            throw new RuntimeException("Tried to attach " + getClass().getSimpleName() + " to " + context.getClass().getSimpleName() + ". May only attached to " + MainActivity.class.getSimpleName());
        }
        return (MainActivity) getContext();
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = getMainActivity();

        // Make sure that view exists, MainActivity.onCreate finished and the callback hasn't
        // been invoked within the current lifecycle
        if (activity.getSharedServicesSet() != null) {
            v(TAG, "Invoking onResumeWithContext, fragment: %s", getClass().getSimpleName());
            onResumeWithContext(activity);
        } else {
            e(TAG, "Skipping onResumeWithContext because context is unavailable, fragment: %s", getClass().getSimpleName());
        }
    }

    @Override
    public void onPause() {
        v(TAG, "onPause, fragment: %s", getClass().getSimpleName());
        if (getContext() != null) {
            onPauseWithContext(getMainActivity());
        } else {
            e(TAG, "Skipping onPauseWithContext because context is unavailable, fragment: %s", getClass().getSimpleName());
        }
        super.onPause();
    }

    protected void toast(@StringRes int text) {
        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }
}
