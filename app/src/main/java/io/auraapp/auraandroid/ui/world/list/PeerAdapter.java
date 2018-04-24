package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.common.lists.ExpandableRecyclerAdapter;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class PeerAdapter extends ExpandableRecyclerAdapter {

    private static final String TAG = "@aura/" + PeerAdapter.class.getSimpleName();

    private final static int TYPE_PEER = 244;
    private final static int TYPE_SPACER = 245;
    private final OnAdoptCallback mOnAdoptCallback;
    private final Context mContext;
    private Timer.Timeout mRedrawTimeout;
    private Timer mTimer = new Timer(new Handler());

    private final Comparator<Peer> mComparator = (a, b) -> {
        if (a.mId == b.mId) {
            // Avoids duplicates when the name changes
            return 0;
        }
        if (Math.abs(a.mLastSeenTimestamp - b.mLastSeenTimestamp) < 60000) {
            if (a.mName == null || b.mName == null) {
                return a.mId - b.mId;
            }
            return a.mName.compareTo(b.mName);
        }
        return (int) (a.mLastSeenTimestamp - b.mLastSeenTimestamp);
    };

    public PeerAdapter(Context context, RecyclerView recyclerView, OnAdoptCallback onAdoptCallback) {
        super(context, recyclerView);
        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mItems.add(new SpacerItem());
    }

    public void notifyPeerListChanged(Set<Peer> peerSet) {

        TreeSet<Peer> treeSet = new TreeSet<>(mComparator);
        treeSet.addAll(peerSet);
        ArrayList<Peer> sortedPeers = new ArrayList<>(treeSet);
        d(TAG, "Updating list, peers was %d, is: %d", mItems.size(), sortedPeers.size());

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new PeersDiffCallback(mItems, sortedPeers));
        mItems.clear();
        mItems.addAll(sortedPeers);
        mItems.add(new SpacerItem());
        diff.dispatchUpdatesTo(this);
    }

    public void notifyPeerChanged(Peer peer) {
        d(TAG, "Updating peer %s", peer);
        int position = -1;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) instanceof Peer && ((Peer) mItems.get(i)).mId == peer.mId) {
                position = i;
                break;
            }
        }
        mItems.remove(position);
        mItems.add(position, peer);
        notifyItemChanged(position);
    }

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {
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
        Timer.clear(mRedrawTimeout);
    }

    @NonNull
    @Override
    public ExpandableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SPACER) {
            return new SpacerHolder(mInflater.inflate(R.layout.common_list_spacer, parent, false));
        }
        return new PeerHolder(
                mInflater.inflate(R.layout.world_peer, parent, false),
                mContext,
                mOnAdoptCallback);
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof SpacerItem) {
            return TYPE_SPACER;
        }
        return TYPE_PEER;
    }
}

