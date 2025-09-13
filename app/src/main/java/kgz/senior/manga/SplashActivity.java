package kgz.senior.manga;

import android.content.Intent;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private static final int SPLASH_DURATION = 3000; // Время показа (мс)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Скрываем статус-бар и навигацию
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surfaceTexture, int width, int height) {
                Surface surface = new Surface(surfaceTexture);
                playVideo(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surfaceTexture, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surfaceTexture) {}
        });

        // Таймер для перехода в главное меню
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DURATION);
    }

    private void playVideo(Surface surface) {
        try {
            mediaPlayer = new MediaPlayer();
            Uri videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash);
            mediaPlayer.setDataSource(this, videoPath);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(false); // Видео играет 1 раз
            mediaPlayer.setOnPreparedListener(mp -> {
                scaleVideo(mp); // Растягиваем видео на экран
                mp.start();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scaleVideo(MediaPlayer mp) {
        float videoWidth = mp.getVideoWidth();
        float videoHeight = mp.getVideoHeight();
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();

        float scaleX = viewWidth / videoWidth;
        float scaleY = viewHeight / videoHeight;
        float scale = Math.max(scaleX, scaleY); // Растягиваем без черных полос

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale, viewWidth / 2, viewHeight / 2);
        textureView.setTransform(matrix);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
