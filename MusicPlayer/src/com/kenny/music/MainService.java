package com.kenny.music;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.kenny.classes.MusicItem;
import com.kenny.utils.MemoryCache;
import com.kenny.utils.MusicUtil;

public class MainService extends Service 
{
	private final String CARD_ID="my_music_card";
	//Constant for pressing previous. If song is more than 5000 Milliseconds (5 seconds), the track will start over, other wise, it will go back a track
	private final int PREVIOUS_TRACKER_MIN=1000*5;
	private LiveCard mLiveCard;
	private MediaPlayer mMediaPlayer;
	private TimelineManager mTimelineManager;
	private List<MusicItem>songs;
	private int songIndex=0;
	private Handler handler = new Handler();
	private final IBinder mBinder = new LocalBinder();
	private boolean paused=false;	
	private MusicRender render;
	private MemoryCache cache;
	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}
	public class LocalBinder extends Binder 
	 {
		 MainService getService() 
		 {
	            return MainService.this;
	     }
	 }
	@Override
    public void onCreate() 
	{
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
        mMediaPlayer= new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() 
        {			
			@Override
			public void onCompletion(MediaPlayer mp) 
			{
				//When the song finished, increment songIndex
				songIndex++;
				if(songIndex-1<songs.size())
				{
					try
					{
						if(handler!=null)
						{
							 handler.removeCallbacks(updateTask);
						}
						//Play the next song
						mMediaPlayer.reset();
						mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
						mMediaPlayer.prepare();
			        	mMediaPlayer.start();
			        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
			        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
			 	        if(bm==null)
			 	        {
			 	        	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
			 	        }
			 	        else
				 	    {
				 	    	render.setAlbumArtwork(bm);
				 	    }
			 	        handler.postDelayed(updateTask, 250);
					}
					catch (Exception e)
					{
						
					}
				}
			}
		});
        songs = new ArrayList<MusicItem>();
        //We will use 1/8th of our memory or cache
        cache= new MemoryCache(8);
        //Our path is hard coded for now
        File dir =new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/MyMusic");              
        for(File f:dir.listFiles())
        {
        	String path=f.getAbsolutePath();
        	MusicItem mi= new MusicItem(path);
        	mi.setAlbum(MusicUtil.getAlbum(path));
        	mi.setArtist(MusicUtil.getArtist(path));
        	mi.setTitle(MusicUtil.getSongTitle(path));
        	mi.setDuration(MusicUtil.getDuration(path));
        	songs.add(mi);
        }
    }
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{		
		if (mLiveCard == null) 
	    {
			mLiveCard = mTimelineManager.getLiveCard(CARD_ID);
	        mLiveCard.setNonSilent(true);
	        // Display the options menu when the live card is tapped.
	        Intent menuIntent = new Intent(this, MenuActivity.class);
	        menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));	  
	        render = new MusicRender(getApplicationContext());
	        //We key our bitmaps on the hash of their location
	        Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
	        if(bm==null)
	        {
	        	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
	        }
	        else
	 	    {
	 	    	render.setAlbumArtwork(bm);
	 	    }
	        mLiveCard.enableDirectRendering(true).getSurfaceHolder().addCallback(render);
	        //Immediately play the first song
	        try
	        {
	        	mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
	        	mMediaPlayer.prepare();
	        	mMediaPlayer.start();
	        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
	        	handler.postAtTime(updateTask, 250);
	        }
	        catch (Exception e)
	        {
	        	
	        }
	        mLiveCard.publish();
	    }
		return START_STICKY;
	}
	/***
	 * Get the MediaPlayer object associated with playing music. 
	 * @return MediaPlayer
	 */
	public MediaPlayer getMediaPlayer()
	{
		return mMediaPlayer;
	}
	@Override
	public void onDestroy() 
	{
		//Empty our song list
		songs.clear();
		songs=null;
		//unpublish our live card
		if (mLiveCard != null) 
		{
			mLiveCard.unpublish();
		    mLiveCard = null;
		}
		//Stop our MediaPlayer and release its resources
		mMediaPlayer.stop();
		mMediaPlayer.release();
		mMediaPlayer=null;
		if(handler!=null)
		{
			 handler.removeCallbacks(updateTask);
			 handler=null;
		}
		if(cache!=null)
		{
			cache.clearCache();
		}
		super.onDestroy();
	}
	/***	 
	 * @return If the music is currently paused
	 */
	public boolean isPaused()
	{		
		return paused;
	}	
	/***
	 * Pauses music if it is currently playing
	 */
	public void pauseMusic()
	{
		if(!paused)
		{
			if(handler!=null)
			{
				 handler.removeCallbacks(updateTask);				
			}
			paused=true;
			mMediaPlayer.pause();
		}
	}
	/***
	 * Resumes the music if it is currently paused
	 */
	public void resumeMusic()
	{
		if(paused)
		{
			paused=false;
			mMediaPlayer.start();
			handler.postAtTime(updateTask, 250);
		}
	}
	/***
	 * Goes back to the previous track
	 */
	public void previousTrack()
	{
		//Makes sure we aren't on the first song
		if(songIndex>0)
		{
			//If they are above the minimum time, start the track over
			if(mMediaPlayer.getCurrentPosition()>PREVIOUS_TRACKER_MIN)
			{
				try
				{
					if(handler!=null)
					{
						 handler.removeCallbacks(updateTask);
					}
					mMediaPlayer.stop();
					mMediaPlayer.reset();
					mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
					mMediaPlayer.prepare();
		        	mMediaPlayer.start();
		        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
		        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
			 	    if(bm==null)
			 	    {
			 	    	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
			 	    }
			 	    else
			 	    {
			 	    	render.setAlbumArtwork(bm);
			 	    }
		        	handler.postAtTime(updateTask, 250);
				}
				catch (Exception e)
				{
					
				}
			}
			//If they are below the minimum, go back a track
			else
			{
				try
				{			
					if(handler!=null)
					{
						 handler.removeCallbacks(updateTask);
					}
					songIndex--;
					mMediaPlayer.stop();
					mMediaPlayer.reset();
					mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
					mMediaPlayer.prepare();
		        	mMediaPlayer.start();
		        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
		        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
			 	    if(bm==null)
			 	    {
			 	    	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
			 	    }
			 	   else
			 	    {
			 	    	render.setAlbumArtwork(bm);
			 	    }
		        	handler.postAtTime(updateTask, 250);
				}
				catch (Exception e)
				{
					
				}
			}
		}
		else
		{
			try
			{
				if(handler!=null)
				{
					 handler.removeCallbacks(updateTask);
				}
				songIndex=0;
				mMediaPlayer.stop();
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
				mMediaPlayer.prepare();
	        	mMediaPlayer.start();
	        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
	        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
		 	    if(bm==null)
		 	    {
		 	    	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
		 	    }
		 	    else
		 	    {
		 	    	render.setAlbumArtwork(bm);
		 	    }
	        	handler.postAtTime(updateTask, 250);
			}
			catch (Exception e)
			{
				
			}
		}
	}
	/***
	 * Goes to the next track
	 */
	public void nextTrack()
	{
		//Make sure we are in our limits of the list
		if(songIndex-1<songs.size())
		{
			try
			{		
				if(handler!=null)
				{
					 handler.removeCallbacks(updateTask);
				}
				songIndex++;
				mMediaPlayer.stop();
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
				mMediaPlayer.prepare();
	        	mMediaPlayer.start();
	        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
	        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
		 	    if(bm==null)
		 	    {
		 	    	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
		 	    }
		 	    else
		 	    {
		 	    	render.setAlbumArtwork(bm);
		 	    }
	        	handler.postAtTime(updateTask, 250);
			}
			catch (Exception e)
			{
				
			}
		}
		//If we are at the end of the list, go back to the beginning
		else
		{
			try
			{	
				if(handler!=null)
				{
					 handler.removeCallbacks(updateTask);
				}
				songIndex=0;
				mMediaPlayer.stop();
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(songs.get(songIndex).getLocation());
				mMediaPlayer.prepare();
	        	mMediaPlayer.start();
	        	render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
	        	Bitmap bm=cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));	
		 	    if(bm==null)
		 	    {
		 	    	new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
		 	    }
		 	    else
		 	    {
		 	    	render.setAlbumArtwork(bm);
		 	    }
	        	handler.postAtTime(updateTask, 250);
			}
			catch (Exception e)
			{
				
			}
			
		}
	}
	/***
	 * Converts milliseconds to a Human readable format
	 * @param milliseconds time in milliseconds
	 * @return Human readable representation of the time
	 */
	private String getHumanReadableTime(long milliseconds)
	{
		String finalTimerString = "";
		String secondsString = "";	  
	    int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
	    int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
	    if(seconds < 10)
	    {
	    	secondsString = "0" + seconds;
	    }
	    else
	    {
	    	secondsString = "" + seconds;
	    }
	    finalTimerString = finalTimerString + minutes + ":" + secondsString;
	    return finalTimerString;
	}
	/***
	 * Thread which we will update our songs time in
	 */
	private Runnable updateTask = new Runnable() 
	{		
		@Override
		public void run() 
		{
			String curr=getHumanReadableTime(mMediaPlayer.getCurrentPosition());		
			render.setTextOfView(null, null, curr);
			//we will update it every quarter of a second
			handler.postDelayed(this, 250);
		}
	};
	/***
	 * Helper class to load Album Art in the background and display it
	 * @author Kenny
	 *
	 */
	private class LoadAlbumBitmap extends AsyncTask<String,Void,Bitmap>
	{
		@Override
		protected Bitmap doInBackground(String... params) 
		{
			Bitmap bm=MusicUtil.getAlbumArtWork(params[0]);
			if(bm!=null)
			{
				cache.addBitmapToMemoryCache(String.valueOf(params[0].hashCode()), bm);
			}			
			return bm;
		}
		@Override
		protected void onPostExecute(Bitmap result) 
		{
			if(result!=null)
			{
				if(render!=null)
				{
					render.setAlbumArtwork(result);
				}
			}
			else
			{
				if(render!=null)
				{
					render.setAlbumArtwork(R.drawable.no_art);
				}
			}
			super.onPostExecute(result);;
		}
	}
}
