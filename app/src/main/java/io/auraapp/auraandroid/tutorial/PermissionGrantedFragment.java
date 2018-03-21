package io.auraapp.auraandroid.tutorial;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ScreenPager;
import io.auraapp.auraandroid.common.EmojiHelper;

public class PermissionGrantedFragment extends Fragment {

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

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_screen_permission_granted, container, false);

        ((TextView) view.findViewById(R.id.granted_emoji)).setText(EmojiHelper.replaceShortCode(":grinning_face:"));
        ((TextView) view.findViewById(R.id.granted_text)).setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_permissionsMissing_granted_text)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(() -> {
//            mPager.getScreenAdapter().removePermissionMissingFragment();
        }, 10000);
    }
}
