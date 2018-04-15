package io.auraapp.auraandroid.ui;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;

public class ActivityState {
    public DialogManager mDialogManager;
    public MyProfileManager mMyProfileManager;
    public Set<Peer> mPeers = new HashSet<>();
    public ScreenPager mPager;
    public TutorialManager mTutorialManager;
    public CommunicatorState mCommunicatorState;
}
