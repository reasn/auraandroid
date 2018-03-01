package io.auraapp.auraandroid.common;

import android.support.annotation.NonNull;

public class EmojiHelper {
    public static String replaceShortCode(@NonNull String input) {
        return input.replaceAll(":fire:", "🔥")
                .replaceAll(":thought_balloon:", "💭")
                .replaceAll(":pencil:", "📝")
                .replaceAll(":ghost:", "👻")
                .replaceAll(":heavy_plus_sign:", "➕");
    }
}
