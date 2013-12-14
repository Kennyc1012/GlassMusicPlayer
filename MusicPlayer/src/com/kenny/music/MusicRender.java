package com.kenny.music;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
public class MusicRender implements SurfaceHolder.Callback
{
	private SurfaceHolder surfaceHolder;
	private TextView title,artist,time;
	private ImageView albumArt;
	private View view;
	/***
	 * Renders the current song title, album artwork, and artist of the current song
	 * @param context context of the view
	 */
	public MusicRender(Context context)
	{
		view=LayoutInflater.from(context).inflate(R.layout.card, null);
		view.setWillNotDraw(false);
		title=(TextView)view.findViewById(R.id.title);
		artist=(TextView)view.findViewById(R.id.artist);
		time=(TextView)view.findViewById(R.id.time);
		albumArt=(ImageView)view.findViewById(R.id.albumArt);
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) 
	{
		 int measuredWidth = View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY);
		 int measuredHeight = View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY);
		 view.measure(measuredWidth, measuredHeight);
		 view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		 draw();
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		surfaceHolder=holder;
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		surfaceHolder=null;	
	}
	/***
	 * Redraw the view to the surface
	 */
	private synchronized void draw() 
	{
		Canvas canvas = null;
	    try 
	    {
	    	canvas = surfaceHolder.lockCanvas();
	    } 
	    catch (RuntimeException e) 
	    {
	    	
	    }
	    if (canvas != null) 
	    {
	    	canvas.drawColor(0, Mode.CLEAR);
	    	view.draw(canvas);
		    try 
		    {
		    	surfaceHolder.unlockCanvasAndPost(canvas);
		    } 
		    catch (RuntimeException e) 
		    {
		    	
		    }
	    }
	}
	/***
	 * Set the text of the Title, Artist, and Time. Passing null as a value will result in the text not being changed
	 * @param title
	 * @param artist
	 * @param time
	 */
	public void setTextOfView(String title,String artist,String time)
	{
		if(title!=null)
		{
			this.title.setText(title);
		}
		if(artist!=null)
		{
			this.artist.setText(artist);
		}
		if(time!=null)
		{
			this.time.setText(time);
		}
		draw();
	}
	/***
	 * Set the text of the Title, Artist, and Time. Passing -1 as a value will result in the text not being changed
	 * @param title
	 * @param artist
	 * @param time
	 */
	public void setTextOfView(int title,int artist,int time)
	{
		if(title!=-1)
		{
			this.title.setText(title);
		}
		if(artist!=-1)
		{
			this.artist.setText(artist);
		}
		if(time!=-1)
		{
			this.time.setText(time);
		}
		draw();
	}
	/***
	 * Sets the image of the album artwork
	 * @param bitmap
	 */
	public void setAlbumArtwork(Bitmap bitmap)
	{
		if(bitmap!=null)
		{
			albumArt.setImageBitmap(bitmap);
		}
		draw();
	}
	/***
	 * Sets the image of the album artwork
	 * @param resId id of the image
	 */
	public void setAlbumArtwork(int resId)
	{
		albumArt.setImageResource(resId);
		draw();
	}
}
