package net.sapium.livestreamprocessor.utils;

import java.io.File;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

public class Timelapser extends Processor {

    public static final int TASK_TIMELAPSE = 1;
    
    private File inFile;
    private double speedupFactor;
    private int audioOption;
    private File audioFile;

    public Timelapser(ProgressChangedListener listener) {
        super(listener);

        registerTask(TASK_TIMELAPSE);
    }

    public Timelapser(ProgressChangedListener listener, File inFile, File outFile, double speedupFactor) {
        this(listener);
        this.inFile = inFile;
        this.setOutFile(outFile);
        this.speedupFactor = speedupFactor;
        this.audioOption = MediaTimelapser.AUDIO_REMOVE;
    }
    
    public Timelapser(ProgressChangedListener listener, File inFile, File outFile, double speedupFactor, int audioOption, File audioFile) {
        this(listener, inFile, outFile, speedupFactor);
        this.audioOption = audioOption;
        this.audioFile = audioFile;
    }
    
    public void setInFile(File inFile){
        this.inFile = inFile;
    }
    
    public void setSpeedupFactor(double speedupFactor){
        this.speedupFactor = speedupFactor;
    }

    @Override
    public boolean validate(int task) {
        if(speedupFactor <= 0){
            getLogger().error("Speed up factor is invalid: " + speedupFactor);
            return false;
        }
        
        if (!inFile.isFile() || !inFile.exists()) {
            getLogger().error("In file could not be read: " + inFile.getAbsolutePath());
            return false;
        }

        if (validateOutFile() == null) {
            getLogger().error("Out file could not be validated: " + getOutFile().getAbsolutePath());
            return false;
        }
        
        if(audioOption == MediaTimelapser.AUDIO_REPLACE){
            if(!audioFile.isFile() || !audioFile.exists()) {
                getLogger().error("Audio file could not be read: " + audioFile.getAbsolutePath());
                return false;
            }
        }
        
        if(audioOption != MediaTimelapser.AUDIO_REMOVE && audioOption != MediaTimelapser.AUDIO_REPLACE && audioOption != MediaTimelapser.AUDIO_SPEED_UP){
            getLogger().error("Invalid audio option: " + audioOption);
            return false;
        }

        return true;
    }

    @Override
    public void process(int task) {
        if (task == TASK_TIMELAPSE) {
            MediaTimelapser timelapseAdapter = new MediaTimelapser(speedupFactor, audioOption);
            VideoData inVid = new VideoData(inFile);
            IMediaReader reader = inVid.getReader();

            reader.addListener(timelapseAdapter);

            IMediaWriter writer = ToolFactory.makeWriter(getOutFile().getAbsolutePath());
            writer.addVideoStream(0, 1, inVid.getWidth(), inVid.getHeight());
            if(audioOption == MediaTimelapser.AUDIO_SPEED_UP || audioOption == MediaTimelapser.AUDIO_REPLACE){
                writer.addAudioStream(1, 1, inVid.getAudioChannels(), inVid.getAudioSampleRate());
            }
            timelapseAdapter.addListener(writer);

            while (reader.readPacket() == null)
                ;
            
            if(audioOption == MediaTimelapser.AUDIO_REPLACE){
                VideoData audio = new VideoData(audioFile);
                IMediaReader audioReader = audio.getReader();
                
                audioReader.addListener(timelapseAdapter);
                
                while (audioReader.readPacket() == null)
                    ;
            }

            writer.close();
        }
    }
}
