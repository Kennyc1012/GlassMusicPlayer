package com.kenny.classes;
/***
 * Helper class for our Music files
 * @author Kenny
 *
 */
public class MusicItem 
{
	private String location,title,artist,album;
	private int duration;
	private long id;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public MusicItem(String fileLocation)
	{
		location=fileLocation;
	}
	public String getLocation() 
	{
		return location;
	}
	public void setLocation(String location) 
	{
		this.location = location;
	}
	public String getTitle() 
	{
		return title;
	}
	public void setTitle(String title) 
	{
		this.title = title;
	}
	public String getArtist() 
	{
		return artist;
	}
	public void setArtist(String artist) 
	{
		this.artist = artist;
	}
	public String getAlbum() 
	{
		return album;
	}
	public void setAlbum(String album) 
	{
		this.album = album;
	}
	public int getDuration() 
	{
		return duration;
	}
	public void setDuration(int duration) 
	{
		this.duration = duration;
	}	
}
