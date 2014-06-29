package com.RFR.glass.racehud.racehud;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.Runnable;

/**
 * This activity manages the options menu that appears when the user taps on the compass's live
 * card.
 */
public class RaceMenuActivity extends Activity
{
    /** For logging. */
    private static final String TAG = "RaceMenu";

    private final Handler mHandler = new Handler();
    private View content = null;

    private boolean mAttachedToWindow;
    private boolean mOptionsMenuOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_view);
        content = findViewById(android.R.id.content);
        Log.d(TAG, "View Created");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        openOptionsMenu();
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
    }

    @Override
    public void openOptionsMenu()
    {
        if (!mOptionsMenuOpen && mAttachedToWindow)
        {
            Log.d(TAG, "Menu Opened");
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        /** Inflate the menu; this adds items to the action bar if it is present. */
        Log.d(TAG, "Menu Created");
        getMenuInflater().inflate(R.menu.race_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.stop:
                /**
                 * Stop the service at the end of the message queue for proper options menu
                 * animation. This is only needed when starting an Activity or stopping a Service
                 * that published a LiveCard.
                 */

                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        stopService(new Intent(RaceMenuActivity.this, RaceService.class));
                    }
                });
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu)
    {
        super.onOptionsMenuClosed(menu);
        mOptionsMenuOpen = false;

        /**
         * We must call finish() from this method to ensure that the activity ends either when an
         * item is selected from the menu or when the menu is dismissed by swiping down.
         */
        finish();
    }
}
