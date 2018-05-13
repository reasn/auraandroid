package io.auraapp.auraandroid.ui.panic;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.auraapp.auraandroid.common.AuraPrefs;

/**
 * Thanks https://github.com/TeamNewPipe/NewPipe/pull/133/files
 */
public class PanicResponderActivity extends Activity {

    public static final String PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && PANIC_TRIGGER_ACTION.equals(intent.getAction())) {
            if (AuraPrefs.shouldPanicSwipe(this)) {
                AuraPrefs.swipe(this);
            }
            if (AuraPrefs.shouldPanicUninstall(this)) {
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                uninstallIntent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(uninstallIntent);
            }

            ExitActivity.exitAndRemoveFromRecentApps(this);
        }
        finishAndRemoveTask();
    }
}