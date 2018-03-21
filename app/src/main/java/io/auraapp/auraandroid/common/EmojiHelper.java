package io.auraapp.auraandroid.common;

public class EmojiHelper {
    public static String replaceShortCode(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll(":fire:", "🔥")
                .replaceAll(":thought_balloon:", "💭")
                .replaceAll(":lollipop:", "🍭")
                .replaceAll(":pencil:", "📝")
                .replaceAll(":warning:", "⚠️")
                .replaceAll(":eyes:", "👀")
                .replaceAll(":sun_behind_small_cloud:", "🌤")
                .replaceAll(":hourglass:", "⏳")
                .replaceAll(":dizzy_face:", "😵")
                .replaceAll(":grinning_face:", "😀")
                .replaceAll(":sleeping_face:", "😴")
                .replaceAll(":sleeping_sign:", "💤")
                .replaceAll(":broken_heart:", "💔")
                .replaceAll(":silhouette_head:", "👤")
                .replaceAll(":memo:", "📝")
                .replaceAll(":sunglasses:", "😎")
                .replaceAll(":see_no_evil:", "🙈")
                .replaceAll(":satellite_antenna:", "📡")
                .replaceAll(":wastebasket:", "🗑")
                .replaceAll(":ghost:", "👻")
                .replaceAll(":heavy_plus_sign:", "➕");
    }
}
