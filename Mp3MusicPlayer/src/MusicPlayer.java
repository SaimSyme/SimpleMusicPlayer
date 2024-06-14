import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.*;
import java.util.ArrayList;

public class MusicPlayer extends PlaybackListener {
    // will be used to update the is_paused more synchronously
    private static final Object playSignal = new Object();

    // need reference so we can update the gui in this class
    private MusicPlayerGUI musicPlayerGUI;

    // making a class to store songs details
    private Song currentSong;
    public Song getCurrentSong(){
        return currentSong;
    }

    private int currentPlaylistIndex;

    private ArrayList<Song> playlist;

    // using JLayer library to create  an advancedPlayer obj which will handle playing the music
    private AdvancedPlayer advancedPlayer;

    // pause boolean flag used to indicate whether the player has been paused
    private boolean isPaused;

    // boolean flag used to tell when the song has been finished
    private boolean songFinished;
    private boolean pressedNext, pressedPrev;

    // stores in the last frame when the playback is finished (used for pausing and resuming)
    private int currentFrame;
    public void setCurrentFrame(int frame){
        currentFrame = frame;
    }

    // track how many millisecond has passed since playing the code (used for updating the slider )
    private int currentTimeInMilli;
    public void setCurrentTimeInMilli( int timeInMilli){
        currentTimeInMilli = timeInMilli;
    }

    // constructor
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI){
        this.musicPlayerGUI = musicPlayerGUI;
    }

    public void loadSong(Song song){
        currentSong = song;
        playlist = null;

        // stop the song if possible
        if(!songFinished)
            stopSong();

        // play the current song if not null
        if(currentSong != null){
            // reset frame
            currentFrame = 0;

            // reset current time in milli
            currentTimeInMilli = 0;

            // update GUI
            musicPlayerGUI.setPlaybackSliderValue(0);

            playCurrentSong();
        }
    }

    public void loadPlaylist(File playlistFile){
        playlist = new ArrayList<>();

        // store the paths from the text file into the playlist array list
        try{
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // reach each line from the text file and store the text into the songpath variable
            String songPath;
            while ((songPath = bufferedReader.readLine()) != null){
                // create song objects based on song path
                Song song = new Song(songPath);

                // add to the playlist array list
                playlist.add(song);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        if(playlist.size() > 0){
            // reset playback slider
            musicPlayerGUI.setPlaybackSliderValue(0);
            currentTimeInMilli = 0;

            //update current song to the first song in the playlist
            currentSong = playlist.get(0);

            // start from the beginnning frame
            currentFrame = 0;

            // update GUI
            musicPlayerGUI.enablePauseButtonDisablePlayButton();
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);
            musicPlayerGUI.updatePlaybackSlider(currentSong);

            // start song
            playCurrentSong();
        }
    }

    public void pauseSong(){
        if(advancedPlayer != null){
            //update ispaused flag
            isPaused = true;

            //then we want to stop the player
            stopSong();
        }
    }

    public void stopSong(){
            if(advancedPlayer != null){
                advancedPlayer.stop();
                advancedPlayer.close();
                advancedPlayer = null;
            }
    }

    public void nextSong(){
        // no need to go to the next song if there is no playlist
        if(playlist == null) return;

        // check to see if we have reached the end of  the playlist, if so then don't do anything
        if(currentPlaylistIndex + 1 > playlist.size() - 1) return;

        pressedNext = true;

        // stop the song if possible
        if(!songFinished)
            stopSong();

        // increase the curent playlist Index
        currentPlaylistIndex++;

        // update current song
        currentSong = playlist.get(currentPlaylistIndex);

        // reset frame
        currentFrame = 0;

        // reset current time in milli
        currentTimeInMilli = 0;

        //update gui
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        //update the song
        playCurrentSong();
    }

    public void prevSong(){
        // no need to go to the next song if there is no playlist
        if(playlist == null) return;

        // check to see if we have previous songs to go to
        if(currentPlaylistIndex - 1 < 0) return;

        pressedPrev = true;

        // stop the song if possible
        if(!songFinished)
            stopSong();

        // decrease the curent playlist Index
        currentPlaylistIndex--;

        // update current song
        currentSong = playlist.get(currentPlaylistIndex);

        // reset frame
        currentFrame = 0;

        // reset current time in milli
        currentTimeInMilli = 0;

        //update gui
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        //update the song
        playCurrentSong();
    }

    public void playCurrentSong(){
        if( currentSong ==  null) return;

        try{
            //read mp3 data
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            //create a new adavnced player
            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            //start music
            startMusicThread();

            // start playback slider thread
            startPlaybackSliderThread();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // create a thread that will handle playing the music
    private void startMusicThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if(isPaused){
                        synchronized (playSignal){
                            // update flag
                             isPaused = false;

                            // notify the other thread to continue (makes sure that ispaused is updated to false property)
                            playSignal.notify();
                        }

                        // resume music from last frame
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);

                    }else {
                        // play music from begining
                        advancedPlayer.play();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // create ta thread that will handle updating the slider
    private void startPlaybackSliderThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                if(isPaused){
                    try{
                        // wait till it get notified by other threads to continue
                        // makes sure the ispause bool flag is updated to false before continuing
                        synchronized (playSignal){
                            playSignal.wait();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                while(!isPaused && !songFinished && !pressedNext && !pressedPrev){
                    try{
                        // increment current time milli
                        currentTimeInMilli++;

                        // calculate into frame value
                        int calculatedFrame = (int) ((double) currentTimeInMilli * 2.08 * currentSong.getFrameRatePerMillisecond());

                        // update the gui
                        musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

                        // mimic 1 milli sec using thread.sleep
                        Thread.sleep(1);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
            // this method get called in the beginning of the song
        System.out.println("Playback Started");
        songFinished =  false;
        pressedNext = false;
        pressedPrev = false;
    }



    @Override
    public void playbackFinished(PlaybackEvent evt) {
            // this method get called when the song finishes
        System.out.println("Playback Finished");
        if(isPaused){
            currentFrame += (int) ((double) evt.getFrame()  * currentSong.getFrameRatePerMillisecond());
        }else {
            // if the user pressed next or prev we dont need to execute the rest of the code
            if(pressedNext || pressedPrev ) return;

            // when the songs end
            songFinished = true;

            if(playlist == null){
                //update gui
                musicPlayerGUI.enablePlayButtonDisablePauseButton();
            }else {
                //last song in the playlist
                if(currentPlaylistIndex == playlist.size() - 1){
                    //update GUI
                    musicPlayerGUI.enablePlayButtonDisablePauseButton();
                }else {
                    //go to next song in the playlist
                    nextSong();
                }
            }
        }
    }
}