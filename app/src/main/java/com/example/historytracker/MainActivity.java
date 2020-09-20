package com.example.historytracker;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {

    private ExternalTexture texture;
    private MediaPlayer mediaPlayer;
    private CustomArFragment arFragment;
    private Scene scene;
    private ModelRenderable renderable;
    private boolean isImageDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texture = new ExternalTexture();

        //mediaPlayer = MediaPlayer.create(this, R.raw.video);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(true);

        ModelRenderable
                .builder()
                .setSource(this, R.raw.video_screen)
                .build()
        .thenAccept(modelRenderable -> {
            modelRenderable.getMaterial().setExternalTexture("videoTexture",
                    texture);
            modelRenderable.getMaterial().setFloat4("keyColor",
                    new Color(0.01843f, 1f, 0.098f));

            renderable = modelRenderable;
        });

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        scene = arFragment.getArSceneView().getScene();
        scene.addOnUpdateListener(this::onUpdate);
    }

    private void onUpdate(FrameTime frameTime) {
        if(isImageDetected)
            return;

        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage image : augmentedImages){
            if(image.getTrackingState() == TrackingState.TRACKING){
                if(image.getName().equals("image")){
                    isImageDetected = true;

                    playVideo(image.createAnchor(image.getCenterPose()), image.getExtentX(), image.getExtentZ());
                    break;
                }
            }
        }

    }

    private void playVideo(Anchor anchor, float extentX, float extentZ) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setSurface(texture.getSurface());
            mediaPlayer.setLooping(true);

            String url = "https://www.youtube.com/watch?v=cbm5szuvRyM";
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
            mediaPlayer.start();

            AnchorNode anchorNode = new AnchorNode(anchor);

            texture.getSurfaceTexture().setOnFrameAvailableListener(surfaceTexture -> {
                anchorNode.setRenderable(renderable);
                texture.getSurfaceTexture().setOnFrameAvailableListener(null);
            });

            anchorNode.setWorldScale(new Vector3(extentX, 1f, extentZ));

            scene.addChild(anchorNode);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        mediaPlayer = null;
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }
}