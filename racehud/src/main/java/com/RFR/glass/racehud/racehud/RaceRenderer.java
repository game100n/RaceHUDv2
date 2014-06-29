package com.RFR.glass.racehud.racehud;

import com.google.android.glass.timeline.DirectRenderingCallback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.RotateAnimation;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by game1_000 on 5/24/2014.
 */
public class RaceRenderer implements DirectRenderingCallback
{

    private static final String TAG = RaceRenderer.class.getSimpleName();

    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;
    private RenderThread mRenderThread;

    private final NumberFormat mRaceFormat;
    private final FrameLayout mLayout;
    //private final RaceView mRaceView;
    private final TextView mRaceViewText;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    /** For Animation */
    private final ImageView mNeedle;
    //private double SpeedtoAngle = 80/(90+42);
    //private double SpeedtoAngle = 80/(135);
    //private static double SpeedtoAngle = 0.60606;
    private static double SpeedtoAngle = 0.59259;
    //private static int NeedleRotateX = 214;
    //private static int NeedleRotateY = 220;
    /** 45 deg is the offset the needle starts at */
    float newAngle = 45;
    float oldAngle = 45;

    private final GPSManager mGPSManager;

    private final GPSManager.OnChangedListener mRaceListener =
            new GPSManager.OnChangedListener()
            {
                @Override
                public void onLocationChanged(GPSManager gpsManager) {
                    Location currentlocation = gpsManager.getLocation();
                    Double currentSpeed = gpsManager.getSpeed();

                    if(currentlocation == null)
                    {
                        mRaceViewText.setText("-.-");
                    }
                    else
                    {
                        /** Find Current speed in MPH */
                        double CurrentSpeedMPH = currentSpeed*2.23694;
                        /** Display Digital Speed */
                        mRaceViewText.setText(mRaceFormat.format(CurrentSpeedMPH));

                        /** Set up to display analog speed */
                        Matrix matrix = mNeedle.getImageMatrix();
                        RectF dst = new RectF();
                        matrix.mapRect(dst, new RectF(mNeedle.getDrawable().getBounds()));
                        //Log.d("Test", "Dst " + dst);
                        /** New angle for needle to point to */
                        newAngle = (float) (CurrentSpeedMPH / SpeedtoAngle);
                        //Log.d("newAngle", newAngle);
                        RotateAnimation mRotateAnimate = new RotateAnimation(oldAngle, newAngle, dst.centerX(), dst.centerY());
                        mRotateAnimate.setDuration(5000);
                        /** Set new angle as the old angle now */
                        oldAngle = newAngle;
                        //Log.d("oldAngle", oldAngle);
                        mNeedle.startAnimation(mRotateAnimate);
                    }

                }

            };

    /**
     * Creates a new instance of the {@code CompassRenderer} with the specified context and
     * orientation manager.
     */
    public RaceRenderer(Context context, GPSManager GPSManager)
    {
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.race_view, null);
        mLayout.setWillNotDraw(false);

        //mRaceView = (RaceView) mLayout.findViewById(R.id.raceview);
        mRaceViewText = (TextView) mLayout.findViewById(R.id.raceText);


        mRaceFormat = NumberFormat.getNumberInstance();
        mRaceFormat.setMinimumFractionDigits(0);
        mRaceFormat.setMaximumFractionDigits(1);


        mGPSManager = GPSManager;

        mNeedle = (ImageView) mLayout.findViewById(R.id.raceNeedle);

        //mRaceView.setOrientationManager(mGPSManager);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        /** The creation of a new Surface implicitly resumes the rendering. */
        mRenderingPaused = false;
        mHolder = holder;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mHolder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused)
    {
        mRenderingPaused = paused;
        updateRenderingState();
    }

    /**
     * Starts or stops rendering according to the {@link com.google.android.glass.timeline.LiveCard}'s state.
     */
    private void updateRenderingState()
    {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);

        if (shouldRender != isRendering)
        {
            if (shouldRender)
            {
                mGPSManager.addOnChangedListener(mRaceListener);
                mGPSManager.start();

                if (mGPSManager.hasLocation())
                {
                    Location location = mGPSManager.getLocation();
                    Double speed = mGPSManager.getSpeed();
                }

                mRenderThread = new RenderThread();
                mRenderThread.start();
            } else {
                mRenderThread.quit();
                mRenderThread = null;

                mGPSManager.removeOnChangedListener(mRaceListener);
                mGPSManager.stop();

            }
        }
    }

    /**
     * Requests that the views redo their layout. This must be called manually every time the
     * speed text is updated because this layout doesn't exist in a GUI thread where those
     * requests will be enqueued automatically.
     */
    private void doLayout()
    {
        /**
         * Measure and update the layout so that it will take up the entire surface space
         * when it is drawn.
         */
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
    }

    /**
     * Repaints the speedometer.
     */
    private synchronized void repaint()
    {
        Canvas canvas = null;

        try
        {
            canvas = mHolder.lockCanvas();
        }
        catch (RuntimeException e)
        {
            Log.d(TAG, "lockCanvas failed", e);
        }

        if (canvas != null)
        {
            //doLayout();
            canvas.drawColor(Color.BLACK);
            mLayout.draw(canvas);

            try
            {
                mHolder.unlockCanvasAndPost(canvas);
            }
            catch (RuntimeException e)
            {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }

    /**
     * Redraws the compass in the background.
     */
    private class RenderThread extends Thread
    {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread()
        {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun()
        {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit()
        {
            mShouldRun = false;
        }

        @Override
        public void run()
        {
            while (shouldRun())
            {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0)
                {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}
