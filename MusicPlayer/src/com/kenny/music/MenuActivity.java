package com.kenny.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

import com.kenny.music.MainService.LocalBinder;

public class MenuActivity extends Activity 
{
	private MainService mService;
	private boolean mBound = false;
	private MenuItem play_paused,shuffled;
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.menu, menu);
		//Need reference to this so we can change its value to play or pause based on if the song is playing
		play_paused=menu.findItem(R.id.play_pause);	
		shuffled=menu.findItem(R.id.shuffle);	       
		return true;
	}
	 @Override
     public void onAttachedToWindow() 
	 {
         super.onAttachedToWindow();
         //Bind to our service so we can manipulate theMediaPlayer if needed
         Intent intent = new Intent(this, MainService.class);
         bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
         //Always open the options menu since its the reason why we are in the activity
         openOptionsMenu();
     }
	 /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() 
    {
        @Override
        public void onServiceConnected(ComponentName className,IBinder service) 
        {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if(mService.isPaused())
    		{
    			play_paused.setTitle(R.string.play);
    			play_paused.setIcon(R.drawable.ic_music_play_50);
    		}
    		else
    		{
    			play_paused.setTitle(R.string.pause);
    			play_paused.setIcon(R.drawable.ic_music_pause_50);
    		}
            if(mService.isShuffled())
            {
            	shuffled.setIcon(R.drawable.shuffle_on);
            }
            else
            {
            	shuffled.setIcon(R.drawable.shuffle_off);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) 
        {
            mBound = false;
        }
    };
    @Override
    protected void onStart()
    {
        super.onStart();
        
    }
    @Override
    protected void onStop() 
    {
        super.onStop();
        // Unbind from the service
        if (mBound) 
        {
            unbindService(mConnection);
            mBound = false;
        }
    }   
    @Override
    public void onOptionsMenuClosed(Menu menu)
    {
        super.onOptionsMenuClosed(menu);        
        finish();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch(item.getItemId())
    	{
    	//Stop, the app will end, and music playback with end
    	case R.id.stop:
    		stopService(new Intent(MenuActivity.this,MainService.class));
    		return true;
    	//Next Track
    	case R.id.next:
    		mService.nextTrack();
    		return true;
    	//Previous Track
    	case R.id.prev:
    		mService.previousTrack();
    		return true;
    	//Play or Pause the song
    	case R.id.play_pause:
    		if(mService.isPaused())
    		{    		
    			mService.resumeMusic();
    		}
    		else
    		{
    			mService.pauseMusic();
    		}
    		return true;
    	//Shuffle
    	case R.id.shuffle:
    		mService.shuffleMusic();
    		return true;
    	}
    	return false;
    }
}
