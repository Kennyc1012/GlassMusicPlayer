package com.kenny.utils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
public class MusicUtil 
{
	//Glass resolution constants, Our artwork will take up half of the screen with full height
	private static final int GLASS_WIDTH=320;
	private static final int GLASS_HEIGHT=360;
	/***
	 * Gets an image for the Album Art Work
	 * @param filePath Path of the file to be decoded
	 * @return Bitmap for the album art work, null if none exists
	 */
	public static Bitmap getAlbumArtWork(String filePath)
	{
		try
		{
			MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
			metaRetriver.setDataSource(filePath);
			byte[] album=metaRetriver.getEmbeddedPicture();
			if(album!=null)
			{
				BitmapFactory.Options opts= new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeByteArray(album, 0, album.length,opts);
				opts.inSampleSize=calculateInSampleSize(opts);
				opts.inJustDecodeBounds=false;
				return BitmapFactory.decodeByteArray(album, 0, album.length,opts);
			}
			return null;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	public static int calculateInSampleSize(BitmapFactory.Options options) 
	{
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	    //Our height and width will always be the same since all glass has the same resolution, for now...
	    if (height > GLASS_HEIGHT || width > GLASS_WIDTH) 
	    {	
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > GLASS_HEIGHT&& (halfWidth / inSampleSize) > GLASS_WIDTH) 
	        {
	            inSampleSize *= 2;
	        }
	    }

	    return inSampleSize;
	}
	/***
	 * Returns album title associated with song
	 * @param filePath Path to song
	 * @return Albums title, null on fail
	 */
	public static String getAlbum(String filePath)
	{
		try
		{

			MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
			metaRetriver.setDataSource(filePath);
			return metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	/***
	 * Returns title associated with song
	 * @param filePath Path to song
	 * @return Songs title, null on fail
	 */
	public static String getSongTitle(String filePath)
	{
		try
		{
			MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
			metaRetriver.setDataSource(filePath);
			return metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	/***
	 * Returns duration of the song
	 * @param filePath Path to song
	 * @return Songs duration, -1 on fail
	 */
	public static int getDuration(String filePath)
	{
		try
		{
			MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
			metaRetriver.setDataSource(filePath);
			return Integer.valueOf(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		}
		catch (Exception e)
		{
			return -1;
		}
	}
	/***
	 * Returns artist associated with song
	 * @param filePath Path to song
	 * @return artist of song, null on fail
	 */
	public static String getArtist(String filePath)
	{
		try
		{
			MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
			metaRetriver.setDataSource(filePath);
			return metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
