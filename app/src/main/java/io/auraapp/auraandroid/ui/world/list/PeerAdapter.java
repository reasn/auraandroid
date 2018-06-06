package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.common.lists.ExpandableRecyclerAdapter;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.w;

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
    private Timer.Timeout mRedrawTimeout;
    private Timer mTimer = new Timer(new Handler());
    private PeerSloganHolder.WhatsMyColorCallback mWhatsMyColorCallback;

    public PeerAdapter(Context context, RecyclerView recyclerView, PeerSloganHolder.WhatsMyColorCallback whatsMyColorCallback, OnAdoptCallback onAdoptCallback) {
        super(context, recyclerView);
        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mWhatsMyColorCallback = whatsMyColorCallback;
        mItems.add(new SpacerItem());
        // Expand single peer (=2 because of spacer item)
        mItemCountToExpandEverything = 2;
        mSloganRecyclerViewPool = new RecyclerView.RecycledViewPool();
    }

    public void notifyPeerListChanged(Collection<Peer> peerSet) {
        List<Object> itemsWithoutDuplicatePeers = new ArrayList<>(PeerDuplicateFilter.sortAndFilterDuplicates(peerSet));
        itemsWithoutDuplicatePeers.add(new SpacerItem());

        d(TAG, "Updating list, items count (including SpacerItem) was %d, is: %d", mItems.size(), itemsWithoutDuplicatePeers.size());

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new PeersDiffCallback(mItems, itemsWithoutDuplicatePeers));
        mItems.clear();
        mItems.addAll(itemsWithoutDuplicatePeers);
        diff.dispatchUpdatesTo(this);
    }

    public void notifyPeerChanged(Peer peer) {
        int position = -1;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) instanceof Peer && ((Peer) mItems.get(i)).mId == peer.mId) {
                position = i;
                break;
            }
        }
        if (position == -1) {
            w(TAG, "Not updating, unknown peer: %s, items: %d", peer, mItems.size());
            return;
        }

        d(TAG, "Updating item %d, peer: %s, items: %d", position, peer, mItems.size());
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

