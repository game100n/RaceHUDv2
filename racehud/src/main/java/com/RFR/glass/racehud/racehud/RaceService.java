package com.RFR.glass.racehud.racehud;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;


/**
 * Created by game1_000 on 5/24/2014.
 */
public class RaceService extends Service
{

    /** For logging. */
    private static final String TAG = "RaceService";
    private static final String LIVE_CARD_TAG = "RaceHUD";

    private OrientationManager mOrientationManager;

    private LiveCard mLiveCard;
    private RaceRenderer mRenderer;

    @Override
    public void onCreate()
    {
        super.onCreate();

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mOrientationManager = new OrientationManager(locationManager);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mLiveCard == null)
        {
            Log.d(TAG, "Publishing LiveCard");

            /** Keep track of the callback to remove it before unpublishing */
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new RaceRenderer(this, mOrientationManager);

            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

            /** Display the options menu when the live card is tapped. */
            Intent menuIntent = new Intent(this, RaceMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);

            /** Jump to the LiveCard when API is available */
            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(TAG, "Done publishing LiveCard");
        }

        else
        {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        if (mLiveCard != null && mLiveCard.isPublished())
        {
            Log.d(TAG, "Unpublishing LiveCard");
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mOrientationManager = null;

        super.onDestroy();
    }


}
