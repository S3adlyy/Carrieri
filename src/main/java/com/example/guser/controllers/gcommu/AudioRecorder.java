package com.example.guser.controllers.gcommu;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioRecorder {

    private TargetDataLine targetDataLine;
    private AudioFormat audioFormat;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream byteArrayOutputStream;

    public AudioRecorder() {
        // Format audio: 16kHz, 16 bits, mono, PCM signé
        audioFormat = new AudioFormat(16000, 16, 1, true, false);
    }

    public void startRecording() {
        if (isRecording) return;

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Microphone non supporté");
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            isRecording = true;
            byteArrayOutputStream = new ByteArrayOutputStream();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            });
            recordingThread.start();

            System.out.println("✅ Enregistrement démarré");

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public File stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        targetDataLine.stop();
        targetDataLine.close();

        try {
            recordingThread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            // Sauvegarder l'audio dans un fichier temporaire
            byte[] audioData = byteArrayOutputStream.toByteArray();
            Path tempFile = Files.createTempFile("voice_", ".wav");

            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = new AudioInputStream(bais, audioFormat, audioData.length / audioFormat.getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempFile.toFile());

            System.out.println("✅ Enregistrement terminé: " + tempFile.toString());
            return tempFile.toFile();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}