import com.mpatric.mp3agic.Mp3File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

public class Song {
    private String songTitle, songArist, songLength, filePath;
    private Mp3File mp3File;
    private double frameRatePerMillisecond;

    public Song (String filePath){
        this.filePath = filePath;
        try{

            mp3File = new Mp3File(filePath);
            frameRatePerMillisecond = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds();

            songLength = convertToSongLengthFormat();

            // use Jaudiotagger library red mp3 file info
            AudioFile audiofile = AudioFileIO.read(new File(filePath));

            // read meta data of file
            Tag tag = audiofile.getTag();
            if(tag != null){
                songTitle = tag.getFirst(FieldKey.TITLE);
                songArist = tag.getFirst(FieldKey.ARTIST);

            }else{
                songTitle = "N/A";
                songArist = "N/A";
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String convertToSongLengthFormat(){
        long minutes = mp3File.getLengthInSeconds() / 60;
        long seconds = mp3File.getLengthInSeconds() % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        return formattedTime;
    }
    //getters
    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArist() {
        return songArist;
    }

    public String getSongLength() {
        return songLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public Mp3File getMp3File(){
        return mp3File;
    }

    public double getFrameRatePerMillisecond(){return frameRatePerMillisecond;}
}
