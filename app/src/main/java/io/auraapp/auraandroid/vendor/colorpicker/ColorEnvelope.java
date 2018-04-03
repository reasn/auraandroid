package io.auraapp.auraandroid.vendor.colorpicker;

public class ColorEnvelope {
    private int color;
    private String htmlCode;
    private int[] rgb;

    public ColorEnvelope(int color, String htmlCode, int[] rgb) {
        this.color = color;
        this.htmlCode = htmlCode;
        this.rgb = rgb;
    }

    public int getColor() {
        return this.color;
    }

    public String getColorHtml() {
        return this.htmlCode;
    }

    public int[] getColorRGB() {
        return this.rgb;
    }
}
