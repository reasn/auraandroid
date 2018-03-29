package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.MySloganManager;
import io.auraapp.auraandroid.ui.world.list.MySlogansRecycleAdapter;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class ProfileFragment extends Fragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/profile/fragment";
    private MySlogansRecycleAdapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private Context mContext;
    private ViewGroup mRootView;
    private MySloganManager mMySloganManager;

    public static ProfileFragment create(Context context, MySloganManager mySloganManager) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.mContext = context;
        fragment.mMySloganManager = mySloganManager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_screen_profile, container, false);

        createListView();

        return mRootView;
    }

    private void createListView() {

        mRecyclerView = mRootView.findViewById(R.id.list_view);

        mRecyclerView.setNestedScrollingEnabled(false);

        List<ListItem> builtinItems = new ArrayList<>();

        mRecyclerAdapter = new MySlogansRecycleAdapter(mContext, builtinItems, mRecyclerView);

        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mMySloganManager.addChangedCallback(event -> {
            d(TAG, "My slogans changed");
            mRecyclerAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
        });

        mRecyclerAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
    }
}
