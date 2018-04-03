package io.auraapp.auraandroid.vendor.colorpicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public abstract class FlagView extends RelativeLayout {

    public FlagView(Context context, int layout) {
        super(context);
        initializeLayout(layout);
    }

    private void initializeLayout(int layout) {
        View inflated = LayoutInflater.from(getContext()).inflate(layout, this);
        inflated.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        inflated.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        inflated.layout(0, 0, inflated.getMeasuredWidth(), inflated.getMeasuredHeight());
    }

    protected void visible() {
        setVisibility(View.VISIBLE);
    }

    protected void gone() {
        setVisibility(View.GONE);
    }

    public abstract void onRefresh(ColorEnvelope colorEnvelope);
}