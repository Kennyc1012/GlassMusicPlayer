package com.kenny.music;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.kenny.classes.MusicItem;
import com.kenny.utils.MemoryCache;
import com.kenny.utils.MusicUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainService extends Service {
    private final String CARD_ID = "my_music_card";

    //Constant for pressing previous. If song is more than 5000 Milliseconds (5 seconds), the track will start over, other wise, it will go back a track
    private final int PREVIOUS_TRACKER_MIN = 1000 * 5;

    private LiveCard liveCard;

    private MediaPlayer mediaPlayer;

    private List<MusicItem> songs, shuffledSongs;

    private int songIndex = 0;

    private Handler handler = new Handler();

    private final IBinder binder = new LocalBinder();

    private boolean paused = false, shuffled = false;

    private MusicRender render;

    private MemoryCache cache;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextTrack();
            }
        });
        songs = new ArrayList<MusicItem>();
        //We will use 1/8th of our memory or cache
        cache = new MemoryCache(8);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (liveCard == null) {
            liveCard = new LiveCard(getApplicationContext(), CARD_ID);
            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            final RemoteViews loadingView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.loading_card);
            liveCard.setViews(loadingView);
            //Load our files in the background. I believe that doing this on the main thread caused an issue with my glass
            // that I had to factory reset to to resolve
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        //Some audio may be explicitly marked as not being music
                        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                        //Query the media store to get all mp3 files on our device. This will pick up any mp3 files found from the media scanner
                        String[] projection = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media._ID};
                        Cursor c = getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                projection, selection, null, null);
                        if (c != null) {
                            while (c.moveToNext()) {
                                String path = c.getString(0);
                                MusicItem mi = new MusicItem(path);
                                mi.setId(c.getLong(1));
                                mi.setAlbum(MusicUtil.getAlbum(path));
                                mi.setArtist(MusicUtil.getArtist(path));
                                mi.setTitle(MusicUtil.getSongTitle(path));
                                mi.setDuration(MusicUtil.getDuration(path));
                                songs.add(mi);
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        //Make sure we actually have songs to play
                        if (songs != null && songs.size() > 0) {
                            render = new MusicRender(getApplicationContext());
                            //We key our bitmaps on the hash of their location
                            Bitmap bm = cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));
                            if (bm == null) {
                                new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
                            } else {
                                render.setAlbumArtwork(bm);
                            }
                            liveCard.unpublish();
                            liveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(render);
                            //Immediately play the first song
                            try {
                                mediaPlayer.setDataSource(songs.get(songIndex).getLocation());
                                mediaPlayer.prepare();
                                mediaPlayer.start();
                                render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
                                handler.postAtTime(updateTask, 250);
                                liveCard.publish(LiveCard.PublishMode.REVEAL);
                            } catch (Exception e) {

                            }
                        } else {
                            //If we have no songs to play, fail gracefully
                            loadingView.setTextViewText(R.id.message, getString(R.string.no_songs));
                            loadingView.setViewVisibility(R.id.progressBar, View.GONE);
                            liveCard.setViews(loadingView);
                            //Start a three second timer to kill the program
                            new CountDownTimer(3000, 3000) {
                                @Override
                                public void onTick(long millisUntilFinished) {

                                }

                                @Override
                                public void onFinish() {
                                    stopSelf();
                                }
                            }.start();
                        }
                    } else {
                        //If we have error, fail gracefully
                        loadingView.setTextViewText(R.id.message, getString(R.string.error_loading_music));
                        loadingView.setViewVisibility(R.id.progressBar, View.GONE);
                        liveCard.setViews(loadingView);
                        //Start a three second timer to kill the program
                        new CountDownTimer(3000, 3000) {
                            @Override
                            public void onTick(long millisUntilFinished) {

                            }

                            @Override
                            public void onFinish() {
                                stopSelf();
                            }
                        }.start();
                    }
                    super.onPostExecute(result);
                }
            }.execute();
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        }
        return START_STICKY;
    }

    /**
     * Get the MediaPlayer object associated with playing music.
     *
     * @return MediaPlayer
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void onDestroy() {
        //Clear our shuffled songs list if we have one
        if (shuffledSongs != null) {
            shuffledSongs.clear();
            shuffledSongs = null;
        }
        //Empty our song list
        songs.clear();
        songs = null;
        //unpublish our live card
        if (liveCard != null) {
            liveCard.unpublish();
            liveCard = null;
        }
        //Stop our MediaPlayer and release its resources
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        if (handler != null) {
            handler.removeCallbacks(updateTask);
            handler = null;
        }
        if (cache != null) {
            cache.clearCache();
        }
        super.onDestroy();
    }

    /**
     * @return If the music is currently paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Pauses music if it is currently playing
     */
    public void pauseMusic() {
        if (!paused) {
            if (handler != null) {
                handler.removeCallbacks(updateTask);
            }
            paused = true;
            mediaPlayer.pause();
        }
    }

    /**
     * Resumes the music if it is currently paused
     */
    public void resumeMusic() {
        if (paused) {
            paused = false;
            mediaPlayer.start();
            handler.postAtTime(updateTask, 250);
        }
    }

    /**
     * Goes back to the previous track
     */
    public void previousTrack() {
        //Makes sure we aren't on the first song
        if (songIndex > 0) {
            //If they are above the minimum time, start the track over
            if (mediaPlayer.getCurrentPosition() > PREVIOUS_TRACKER_MIN) {
                try {
                    if (handler != null) {
                        handler.removeCallbacks(updateTask);
                    }
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    Bitmap bm = null;
                    //Get the appropriate song based on if we are shuffled or not
                    if (shuffled) {
                        mediaPlayer.setDataSource(shuffledSongs.get(songIndex).getLocation());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        render.setTextOfView(shuffledSongs.get(songIndex).getTitle(), shuffledSongs.get(songIndex).getArtist(), null);
                        bm = cache.getBitmapFromMemCache(String.valueOf(shuffledSongs.get(songIndex).getLocation().hashCode()));
                    } else {
                        mediaPlayer.setDataSource(songs.get(songIndex).getLocation());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
                        bm = cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));
                    }
                    if (bm == null) {
                        if (shuffled) {
                            new LoadAlbumBitmap().execute(shuffledSongs.get(songIndex).getLocation());
                        } else {
                            new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
                        }
                    } else {
                        render.setAlbumArtwork(bm);
                    }
                    handler.postAtTime(updateTask, 250);
                } catch (Exception e) {

                }
            }
            //If they are below the minimum, go back a track
            else {
                try {
                    if (handler != null) {
                        handler.removeCallbacks(updateTask);
                    }
                    songIndex--;
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    Bitmap bm = null;
                    //Get the appropriate song based on if we are shuffled or not
                    if (shuffled) {
                        mediaPlayer.setDataSource(shuffledSongs.get(songIndex).getLocation());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        render.setTextOfView(shuffledSongs.get(songIndex).getTitle(), shuffledSongs.get(songIndex).getArtist(), null);
                        bm = cache.getBitmapFromMemCache(String.valueOf(shuffledSongs.get(songIndex).getLocation().hashCode()));
                    } else {
                        mediaPlayer.setDataSource(songs.get(songIndex).getLocation());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
                        bm = cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));
                    }
                    if (bm == null) {
                        if (shuffled) {
                            new LoadAlbumBitmap().execute(shuffledSongs.get(songIndex).getLocation());
                        } else {
                            new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
                        }
                    } else {
                        render.setAlbumArtwork(bm);
                    }
                    handler.postAtTime(updateTask, 250);
                } catch (Exception e) {

                }
            }
        } else {
            try {
                if (handler != null) {
                    handler.removeCallbacks(updateTask);
                }
                songIndex = 0;
                mediaPlayer.stop();
                mediaPlayer.reset();
                Bitmap bm = null;
                //Get the appropriate song based on if we are shuffled or not
                if (shuffled) {
                    mediaPlayer.setDataSource(shuffledSongs.get(songIndex).getLocation());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    render.setTextOfView(shuffledSongs.get(songIndex).getTitle(), shuffledSongs.get(songIndex).getArtist(), null);
                    bm = cache.getBitmapFromMemCache(String.valueOf(shuffledSongs.get(songIndex).getLocation().hashCode()));
                } else {
                    mediaPlayer.setDataSource(songs.get(songIndex).getLocation());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
                    bm = cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));
                }
                if (bm == null) {
                    if (shuffled) {
                        new LoadAlbumBitmap().execute(shuffledSongs.get(songIndex).getLocation());
                    } else {
                        new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
                    }
                } else {
                    render.setAlbumArtwork(bm);
                }
                handler.postAtTime(updateTask, 250);
            } catch (Exception e) {

            }
        }
    }

    /**
     * Goes to the next track
     */
    public void nextTrack() {
        //If we are currently shuffling the songs, we choose our next song based on the appropriate list
        if (shuffled) {
            //Make sure we are in our limits of the shuffled list
            if (songIndex + 1 < shuffledSongs.size()) {
                try {
                    if (handler != null) {
                        handler.removeCallbacks(updateTask);
                    }
                    songIndex++;
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(shuffledSongs.get(songIndex).getLocation());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    render.setTextOfView(shuffledSongs.get(songIndex).getTitle(), shuffledSongs.get(songIndex).getArtist(), null);
                    Bitmap bm = cache.getBitmapFromMemCache(String.valueOf(shuffledSongs.get(songIndex).getLocation().hashCode()));
                    if (bm == null) {
                        new LoadAlbumBitmap().execute(shuffledSongs.get(songIndex).getLocation());
                    } else {
                        render.setAlbumArtwork(bm);
                    }
                    handler.postAtTime(updateTask, 250);
                } catch (Exception e) {

                }
            }
            //If we are at the end of the list, kill the application
            else {
                stopSelf();
            }
        } else {
            //Make sure we are in our limits of the list
            if (songIndex + 1 < songs.size()) {
                try {
                    if (handler != null) {
                        handler.removeCallbacks(updateTask);
                    }
                    songIndex++;
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(songs.get(songIndex).getLocation());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    render.setTextOfView(songs.get(songIndex).getTitle(), songs.get(songIndex).getArtist(), null);
                    Bitmap bm = cache.getBitmapFromMemCache(String.valueOf(songs.get(songIndex).getLocation().hashCode()));
                    if (bm == null) {
                        new LoadAlbumBitmap().execute(songs.get(songIndex).getLocation());
                    } else {
                        render.setAlbumArtwork(bm);
                    }
                    handler.postAtTime(updateTask, 250);
                } catch (Exception e) {

                }
            }
            //If we are at the end of the list, kill the application
            else {
                stopSelf();
            }
        }
    }

    /**
     * Converts milliseconds to a Human readable format
     *
     * @param milliseconds time in milliseconds
     * @return Human readable representation of the time
     */
    private String getHumanReadableTime(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }
        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        return finalTimerString;
    }

    /**
     * Thread which we will update our songs time in
     */
    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            String curr = getHumanReadableTime(mediaPlayer.getCurrentPosition());
            render.setTextOfView(null, null, curr);
            //we will update it every quarter of a second
            handler.postDelayed(this, 250);
        }
    };

    /**
     * Helper class to load Album Art in the background and display it
     *
     * @author Kenny
     */
    private class LoadAlbumBitmap extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bm = MusicUtil.getAlbumArtWork(params[0]);
            if (bm != null) {
                cache.addBitmapToMemoryCache(String.valueOf(params[0].hashCode()), bm);
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                if (render != null) {
                    render.setAlbumArtwork(result);
                }
            } else {
                if (render != null) {
                    render.setAlbumArtwork(R.drawable.no_art);
                }
            }
            super.onPostExecute(result);
            ;
        }
    }

    /**
     * @return if the music is being shuffle
     */
    public boolean isShuffled() {
        return shuffled;
    }

    /**
     * Shuffles our music. If the music is already shuffled, it will be turned off. When music
     * gets shuffled, it stops the current playback and starts over with the shuffled list.
     */
    public void shuffleMusic() {
        if (shuffled) {
            try {
                shuffled = false;
                //Get which song is currently being played
                MusicItem item = shuffledSongs.get(songIndex);
                //We will loop and find which position that song is at from our original list
                //The music playback will not stop but just continue from the original list
                for (int i = 0; i < songs.size(); i++) {
                    if (item.getId() == songs.get(i).getId()) {
                        songIndex = i;
                        break;
                    }
                }
                shuffledSongs.clear();
                shuffledSongs = null;
            } catch (Exception e) {

            }
        } else {
            try {
                shuffled = true;
                shuffledSongs = new ArrayList<MusicItem>();
                Random ran = new Random();
                //Shuffling will always take the entire list and shuffle them randomly
                List<MusicItem> temp = new ArrayList<MusicItem>(songs);
                while (temp.size() > 0) {
                    int selected = ran.nextInt(temp.size());
                    shuffledSongs.add(temp.get(selected));
                    temp.remove(selected);
                }
                temp = null;
                songIndex = 0;
                if (handler != null) {
                    handler.removeCallbacks(updateTask);
                }
                //Once we get our shuffled list, stop the music and start it again using the shuffled list
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(shuffledSongs.get(songIndex).getLocation());
                mediaPlayer.prepare();
                mediaPlayer.start();
                render.setTextOfView(shuffledSongs.get(songIndex).getTitle(), shuffledSongs.get(songIndex).getArtist(), null);
                Bitmap bm = cache.getBitmapFromMemCache(String.valueOf(shuffledSongs.get(songIndex).getLocation().hashCode()));
                if (bm == null) {
                    new LoadAlbumBitmap().execute(shuffledSongs.get(songIndex).getLocation());
                } else {
                    render.setAlbumArtwork(bm);
                }
                handler.postAtTime(updateTask, 250);
            } catch (Exception e) {

            }
        }
    }
}
