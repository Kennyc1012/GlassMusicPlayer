GlassMusicPlayer
================
GlassMusicPlayer is an mp3 player for Google Glass. It does not require an internet connection, it plays files off of your internal storage (on glass)


![Screen Shot1](https://github.com/Kennyc1012/GlassMusicPlayer/raw/master/Screenshot_1.png)
![Screen Shot2](https://github.com/Kennyc1012/GlassMusicPlayer/raw/master/Screenshot_2.png)


To use, adb sideload the apk to your glass as any other. 
Your mp3 files MUST be in a folder labeled MyMusic inside of your DCIM folder (so the path should me /mnt/sdcard/DCIM/MyMusic). This path is hard coded for now, but can easily be changed to whatever you desire. 


To trigger the app, say OK Glass...Play Music. 
It will play the songs in the order they appear in the folder structure.
