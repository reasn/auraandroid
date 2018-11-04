package io.auraapp.auraandroid.ui.world.list;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.common.ProductionStubFactory;
import io.auraapp.auraandroid.ui.common.lists.ExpandableRecyclerAdapter;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETE_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPEN_ACTION;

public class PeerAdapter extends ExpandableRecyclerAdapter {

    private static final String TAG = "@aura/ui/world/" + PeerAdapter.class.getSimpleName();

    private final static int TYPE_PEER = 244;
    private final static int TYPE_SPACER = 245;
    private final OnAdoptCallback mOnAdoptCallback;
    private final Context mContext;
    /**
     * All slogan recyclers share one view pool to reduce inflations.
     * Thanks https://medium.com/@mgn524/optimizing-nested-recyclerview-a9b7830a4ba7
     */
    private final RecyclerView.RecycledViewPool mSloganRecyclerViewPool;
    private final PeerSloganHolder.WhatsMyColorCallback mWhatsMyColorCallback;
    private final Timer mTimer = new Timer(new Handler());
    private Timer.Timeout mRedrawTimeout;

    private List<Object> mOriginalPeers;
    private Runnable mUnregisterPrefListener;
    private boolean mTutorialOpen;
    private boolean mFakePeersEnabled;

    private final BroadcastReceiver mTutorialReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOCAL_TUTORIAL_OPEN_ACTION.equals(intent.getAction())) {
                mTutorialOpen = true;
                toggleFakePeers();
            }
            if (LOCAL_TUTORIAL_COMPLETE_ACTION.equals(intent.getAction())) {
                mTutorialOpen = false;
                toggleFakePeers();
            }
        }
    };

    public PeerAdapter(Context context, boolean tutorialOpen, PeerSloganHolder.WhatsMyColorCallback whatsMyColorCallback, OnAdoptCallback onAdoptCallback) {
        super(context);
        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mWhatsMyColorCallback = whatsMyColorCallback;
        mItems.add(new SpacerItem());
        // Expand single peer (=2 because of spacer item)
        mItemCountToExpandEverything = 2;
        mTutorialOpen = tutorialOpen;
        mSloganRecyclerViewPool = new RecyclerView.RecycledViewPool();
        mFakePeersEnabled = AuraPrefs.areDebugFakePeersEnabled(mContext);
    }

    public void notifyPeerListChanged(Collection<Peer> peerSet) {
        mOriginalPeers = new ArrayList<>(peerSet);

        List<Object> itemsWithoutDuplicatePeers;
        if (mFakePeersEnabled) {
            peerSet = new HashSet<>();
            peerSet.addAll(getOriginalPeers());
            peerSet.addAll(ProductionStubFactory.createFakePeers());
        }
        itemsWithoutDuplicatePeers = new ArrayList<>(PeerListHelper.sortAndFilterDuplicates(peerSet));
        itemsWithoutDuplicatePeers.add(new SpacerItem());

        d(TAG, "Updating list, items count (including SpacerItem) was %d, is: %d", mItems.size(), itemsWithoutDuplicatePeers.size());

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new PeersDiffCallback(mItems, itemsWithoutDuplicatePeers));
        mItems.clear();
        mItems.addAll(itemsWithoutDuplicatePeers);
        diff.dispatchUpdatesTo(this);
    }

    public List<Peer> getVisiblePeers() {
        List<Peer> peers = new ArrayList<>(getOriginalPeers());
        if (mFakePeersEnabled) {
            peers.addAll(ProductionStubFactory.createFakePeers());
        }
        return peers;
    }

    public void notifyPeerChanged(Peer peer) {
        PeerListHelper.replace(mOriginalPeers, peer);
        int position = PeerListHelper.replace(mItems, peer);
        if (position == -1) {
            w(TAG, "Not updating, unknown peer: %s, items: %d", peer, mItems.size());
            return;
        }
        notifyItemChanged(position);
    }

    private Collection<Peer> getOriginalPeers() {
        HashSet<Peer> castCollection = new HashSet<>();
        for (Object peer : mOriginalPeers) {
            castCollection.add((Peer) peer);
        }
        return castCollection;
    }

    private void toggleFakePeers() {
        mFakePeersEnabled = mTutorialOpen || AuraPrefs.areDebugFakePeersEnabled(mContext);
        notifyPeerListChanged(getOriginalPeers());
    }

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {

        mUnregisterPrefListener = AuraPrefs.listen(mContext, R.string.prefs_debug_fake_peers_key, value -> toggleFakePeers());
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mTutorialReceiver, IntentFactory.createFilter(LOCAL_TUTORIAL_OPEN_ACTION, LOCAL_TUTORIAL_COMPLETE_ACTION));
        Timer.clear(mRedrawTimeout);
        mRedrawTimeout = mTimer.setSerializedInterval(() -> {
            long now = System.currentTimeMillis();
            for (Object item : mItems) {
                if (item instanceof SpacerItem) {
                    continue;
                }
                if (now - ((Peer) item).mLastSeenTimestamp > 10000) {
                    notifyItemChanged(mItems.indexOf(item));
                }
            }
        }, 1000);
    }

    public void onPause() {
        if (mUnregisterPrefListener != null) {
            mUnregisterPrefListener.run();
        }
        Timer.clear(mRedrawTimeout);
    }

    @NonNull
    @Override
    public ExpandableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SPACER) {
            return new SpacerHolder(mInflater.inflate(R.layout.common_list_spacer, parent, false));
        }
        PeerHolder holder = new PeerHolder(
                mInflater.inflate(R.layout.world_peer, parent, false),
                mContext,
                mOnAdoptCallback,
                mWhatsMyColorCallback);

        holder.setPool(mSloganRecyclerViewPool);
        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof SpacerItem) {
            return TYPE_SPACER;
        }
        return TYPE_PEER;
    }
}

