package io.auraapp.auraandroid.ui.panic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Thanks https://github.com/TeamNewPipe/NewPipe/pull/133/files
 */
public class ExitActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finishAndRemoveTask();
        System.exit(0);
    }

    public static void exitAndRemoveFromRecentApps(Activity activity) {
        Intent intent = new Intent(activity, ExitActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        activity.startActivity(intent);
    }
}