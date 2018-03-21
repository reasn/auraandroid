package io.auraapp.auraandroid.ui.permissions;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PermissionGrantedFragment extends Fragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/permissions/" + PermissionGrantedFragment.class.getSimpleName();
    private Context mContext;
    private ScreenPager mPager;

    public static PermissionGrantedFragment create(Context context, ScreenPager pager) {
        PermissionGrantedFragment fragment = new PermissionGrantedFragment();
        fragment.mContext = context;
        fragment.mPager = pager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_screen_permission_granted, container, false);

        ((TextView) view.findViewById(R.id.granted_emoji)).setText(EmojiHelper.replaceShortCode(":grinning_face:"));
        ((TextView) view.findViewById(R.id.granted_text)).setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_permissionsMissing_granted_text)));

        return view;
    }

    @Override
    public void cameIntoView() {
        mPager.getScreenAdapter().removePermissionMissingFragment();
    }
}
