package io.auraapp.auraandroid.ui.common;

import android.graphics.Color;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

public class ColorHelper {

    /**
     * Calculates the perceived brightness (0-255) of a color
     * Thanks to http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
     * Thanks to http://alienryderflex.com/hsp.html
     */
    public static int getBrightness(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return (int) Math.sqrt(
                r * r * .299
                        + g * g * .587
                        + b * b * .114);
    }

    public static int adjustBrightness(int color, int factor) {
        int r = round(Color.red(color) + factor);
        int g = round(Color.green(color) + factor);
        int b = round(Color.blue(color) + factor);
        return Color.rgb(
                max(0, min(r, 255)),
                max(0, min(g, 255)),
                max(0, min(b, 255)));
    }

    public static int getAccent(int color) {
        int brightness = getBrightness(color);

        return brightness > 128
                ? adjustBrightness(color, -10 - (brightness * 50 / 128 / 2))
                : adjustBrightness(color, 20 + min(40, 1000 / (brightness + 1)));
    }

    public static int getTextColor(int color) {
        return getBrightness(color) > 128
                ? Color.BLACK
                : Color.WHITE;
    }
}
