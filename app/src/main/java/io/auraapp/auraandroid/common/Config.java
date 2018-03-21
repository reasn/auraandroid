package io.auraapp.auraandroid.common;

import io.auraapp.auraandroid.ui.MainActivity;

public class Config {

    // Notifications

    public static final int COMMUNICATOR_FOREGROUND_NOTIFICATION_ID = 1338;
    public static final int COMMON_SLOGAN_MAX_LINE_BREAKS = 1;

    public static int[] PEERS_CHANGED_NOTIFICATION_LIGHT_PATTERN = new int[]{3000, 3000};
    // Assuming that this is the default pattern
    // Thanks to https://stackoverflow.com/questions/37805051/how-to-get-the-default-vibration-pattern-of-an-android-device
    public static long[] PEERS_CHANGED_NOTIFICATION_VIBRATION_PATTERN = new long[]{0, 250, 250, 250};

    // Communicator

    public static final boolean COMMUNICATOR_HIGH_POWER = true;
    public static final long COMMUNICATOR_PEER_FORGET_AFTER = 1000 * 60 * 3;
    public static final long COMMUNICATOR_PEER_CONNECT_TIMEOUT = 1000 * 10;
    public static final long COMMUNICATOR_MY_ID_SHUFFLE_INTERVAL = 60 * 60 * 1000;

    /**
     * The time to remember BT_TURNING_ON events for.
     * Assumption: If Aura is not working properly because there's a problem with the BT stack,
     * users might repeatedly turn BT on and off. If that is the case an explanatory dialog will be shown.
     * This number sets the interval that's being considered "recent" for the number of clicks.
     * I.e. If set to 2 minutes, and within that timeframe the user clicks more
     * than COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD times, an alert is shown in MainActivity
     *
     * @see MainActivity#showBrokenBtStackAlert
     */
    public static final int COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_RECENT_TIMEFRAME = 1000 * 60;
    public static final int COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD = 2;


    // Main

    public static int MAIN_LOOKING_AROUND_SHOW_DURATION = 10000;

    public static final boolean MAIN_DEBUG_VIEW_ENABLED = true;
    public static final int MAIN_DEBUG_VIEW_SWITCH_CLICKS = 5;
    public static final int MAIN_DEBUG_VIEW_SWITCH_INTERVAL = 3000;

    // Common

    public static final int COMMON_SLOGAN_MAX_SLOGANS = 5;
    public static int COMMON_SLOGAN_MAX_LENGTH = 160;
}
