package io.auraapp.auraandroid.ui.common;

import android.graphics.Color;

public class ColorHelper {

    /**
     * Calculates the perceived brightness (0-255) of a color
     * Thanks to http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
     */
    public static int getBrightness(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return (int) Math.sqrt(
                r * r * .241 +
                        g * g * .691 +
                        b * b * .068);
    }
}
