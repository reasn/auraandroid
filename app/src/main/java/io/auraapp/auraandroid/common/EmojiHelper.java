package io.auraapp.auraandroid.common;


public class EmojiHelper {
    public static String replaceAppEmoji(String input) {
        return input.replaceAll(":fire:", "🔥")
                .replaceAll(":thought_balloon:", "💭")
                .replaceAll(":heavy_plus_sign:", "➕");
    }
}
