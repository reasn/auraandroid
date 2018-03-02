package io.auraapp.auraandroid.main.list;

import android.support.v7.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

class ListSynchronizer {

    private static final String TAG = "aura/list/listSynchronizer";

    @FunctionalInterface
    interface ApplicabilityCallback {
        boolean isApplicable(ListItem item);
    }

    @FunctionalInterface
    interface CompareCallback {
        boolean isGreaterThan(ListItem item, ListItem newItem);
    }

    static void syncLists(List<ListItem> items,
                          List<ListItem> newItems,
                          RecyclerView.Adapter<?> notificationReceiver,
                          ApplicabilityCallback applicabilityCallback,
                          CompareCallback compareCallback) {
        d(TAG, "Updating list, mySlogans: %d", newItems.size());

        Set<Runnable> mutations = new HashSet<>();

        // Update changed and remove absent items
        for (ListItem item : items) {

            if (!applicabilityCallback.isApplicable(item)) {
                continue;
            }
            boolean found = false;
            for (ListItem newItem : newItems) {
                if (item.compareIndex(newItem) == 0) {
                    found = true;
                    if (item.equals(newItem)) {
                        int index = items.indexOf(item);
                        items.remove(index);
                        items.add(index, newItem);
                        notificationReceiver.notifyItemChanged(index);
                    }
                    break;
                }
            }
            if (!found) {
                mutations.add(() -> {
                    int index = items.indexOf(item);
                    v(TAG, "Removing item %s at %d", item.getSlogan(), index);
                    items.remove(item);
                    notificationReceiver.notifyItemRemoved(index);
                });
            }
        }

        for (Runnable r : mutations) {
            r.run();
        }
        mutations.clear();

        // Add new items
        for (ListItem newItem : newItems) {
            boolean found = false;
            for (ListItem candidate : items) {
                if (candidate.compareIndex(newItem) == 0) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mutations.add(() -> {
                    int index;
                    // Determine the index of the first item that's supposed to be after newItem
                    for (index = 0; index < items.size(); index++) {
                        ListItem item = items.get(index);
                        if (!applicabilityCallback.isApplicable(item)) {
                            continue;
                        }
                        if (compareCallback.isGreaterThan(item, newItem)) {
                            break;
                        }
                    }
                    v(TAG, "Inserting item %s at %d", newItem.getSlogan(), index);
                    items.add(index, newItem);
                    notificationReceiver.notifyItemInserted(index);
                });
            }
        }

        for (Runnable r : mutations) {
            r.run();
        }
    }
}
