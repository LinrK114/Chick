package com.example.customclickapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "app_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private ImageView centerImage;
    private ImageButton btnTheme;
    private ImageButton btnAbout;

    private SoundPool soundPool;
    private int soundId = -1;
    private boolean soundLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedNightMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        centerImage = findViewById(R.id.centerImage);
        btnTheme = findViewById(R.id.btnTheme);
        btnAbout = findViewById(R.id.btnAbout);

        loadCenterImage();
        prepareSound();
        updateThemeIcon();

        centerImage.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(80)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start())
                    .start();
            playClickSound();
        });

        btnTheme.setOnClickListener(v -> toggleTheme());

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    private void applySavedNightMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void toggleTheme() {
        int current = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        int newMode;
        if (current == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_NIGHT_MODE, newMode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(newMode);
        recreate();
    }

    private void updateThemeIcon() {
        int current = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if (current == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            btnTheme.setImageResource(R.drawable.ic_brightness_2);
        } else {
            btnTheme.setImageResource(R.drawable.ic_wb_sunny);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateThemeIcon();
    }

    private void loadCenterImage() {
        String[] candidates = {
                "center_image.png",
                "center_image.jpg",
                "center_image.jpeg",
                "center_image.webp"
        };

        for (String name : candidates) {
            try (InputStream is = getAssets().open(name)) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    centerImage.setImageBitmap(bitmap);
                }
                return;
            } catch (IOException ignored) {
            }
        }
    }

    private void prepareSound() {
        String[] candidates = {
                "click_sound.mp3",
                "click_sound.ogg",
                "click_sound.wav"
        };

        File cacheFile = null;

        for (String name : candidates) {
            try (InputStream is = getAssets().open(name);
                 FileOutputStream os = new FileOutputStream(
                         cacheFile = new File(getCacheDir(), "click_sound_audio"))) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
                break;
            } catch (IOException e) {
                cacheFile = null;
            }
        }

        if (cacheFile == null || !cacheFile.exists()) {
            return;
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(16)
                .setAudioAttributes(audioAttributes)
                .build();

        soundId = soundPool.load(cacheFile.getAbsolutePath(), 1);
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                soundLoaded = true;
            }
        });
    }

    private void playClickSound() {
        if (soundLoaded && soundPool != null && soundId != -1) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}