package com.example.customclickapp;

import androidx.appcompat.app.AppCompatActivity;   // 【改动1】导入 AndroidX 的 AppCompatActivity
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {   // 【改动2】继承改为 AppCompatActivity

    private ImageView centerImage;
    private SoundPool soundPool;
    private int soundId = -1;
    private boolean soundLoaded = false;
    private File soundCacheFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        centerImage = findViewById(R.id.centerImage);

        // 先加载自定义图片和音效
        loadCenterImage();
        prepareSound();

        centerImage.setOnClickListener(v -> {
            // 1. 视觉反馈：快速缩小再弹回
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

            // 2. 播放音效
            playClickSound();
        });
    }

    /**
     * 从 assets 加载中间小图片。
     * 支持 center_image.png / jpg / jpeg / webp
     * 如果都没有，就保留布局中的默认矢量图。
     */
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
                // 尝试下一个文件名
            }
        }
    }

    /**
     * 从 assets 复制音效到缓存，然后用 SoundPool 加载。
     * 支持 click_sound.mp3 / ogg / wav
     * 如果都没有，点击时只有视觉反馈，不会报错。
     */
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
                break; // 复制成功，停止尝试
            } catch (IOException e) {
                cacheFile = null;
            }
        }

        if (cacheFile == null || !cacheFile.exists()) {
            return; // 没有可用音效
        }

        this.soundCacheFile = cacheFile;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(50)
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