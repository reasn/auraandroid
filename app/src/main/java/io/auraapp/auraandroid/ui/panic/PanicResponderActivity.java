package io.auraapp.auraandroid.ui.panic;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;

/**
 * Thanks https://github.com/TeamNewPipe/NewPipe/pull/133/files
 */
public class PanicResponderActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && Config.PANIC_TRIGGER_ACTION.equals(intent.getAction())) {

            boolean shouldUninstall = AuraPrefs.shouldUninstallOnPanic(this);
            if (AuraPrefs.shouldPurgeOnPanic(this)) {
                AuraPrefs.purge(this);
            }
            AuraPrefs.finish(this);

            if (shouldUninstall) {
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                uninstallIntent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(uninstallIntent);
            }
            ExitActivity.exitAndRemoveFromRecentApps(this);
        }
        finishAndRemoveTask();
    }
}