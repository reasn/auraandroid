package io.auraapp.auraandroid.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

import io.auraapp.auraandroid.R;

public class RabbitView extends View {

    private Movie mMovie;
    private long movieStart;

    public RabbitView(Context context) {
        super(context);
        initializeView();
    }

    public RabbitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public RabbitView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView();
    }

    private void initializeView() {
        InputStream is = getContext().getResources().openRawResource(R.raw.rabbit);
        mMovie = Movie.decodeStream(is);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();

        if (movieStart == 0) {
            movieStart = (int) now;
        }
        if (mMovie != null) {
            int relTime = (int) ((now - movieStart) % mMovie.duration());
            mMovie.setTime(relTime);
            mMovie.draw(canvas, getWidth() - mMovie.width(), getHeight() - mMovie.height());
            this.invalidate();
        }
    }
}