package com.example.lab6_2;



import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
/**
 * Lab 6.1, 6.2 - Tạo giao diện nghe nhạc
 *
 * Đối tượng MusicAdapter để quản lý và hiển thị danh sách bài hát lên màn hình
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final int LEVEL_PAUSE = 0;
    private static final int LEVEL_PLAY = 1;
    private static final MediaPlayer player = new MediaPlayer();

    private static final int STATE_IDE = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;
    private final ArrayList<SongEntity> listSong = new ArrayList<>();

    private TextView tvName, tvAlbum, tvTime;
    private SeekBar seekBar;
    private ImageView ivPlay;

    private int index;
    private SongEntity songEntity;
    private Thread thread;
    private int state = STATE_IDE;
    private String totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        ivPlay = findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_next).setOnClickListener(this);

        tvName = findViewById(R.id.tv_name);
        tvAlbum = findViewById(R.id.tv_album);
        tvTime = findViewById(R.id.tv_time);
        seekBar = findViewById(R.id.sb_seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        //Check quyền truy cập bộ nhớ
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            return;
        }

        //Tải bài hát lên RecyclerView
        loadingListSongOffline();
    }

    /**
     * Lắng nghe phản hồi từ gười dùng
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadingListSongOffline();

        } else {
            Toast.makeText(this, R.string.txt_alert, Toast.LENGTH_SHORT).show();

            finish();
        }
    }

    /**
     * Tải bài bài hát lên RecyclerView
     */
    private void loadingListSongOffline() {
        //ContentResolver cho phép truy cập đến tài nguyên của ứng dụng thông qua 1 đường dẫn uri
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null);
        if (c != null) {
            c.moveToFirst();
            listSong.clear();
            while (!c.isAfterLast()) {
                int nameIndex = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int pathIndex = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                int albumIndex = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    albumIndex = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST);
                }
                String name = "";
                String path = "";
                String album = "";

                if(nameIndex >= 0){
                    name = c.getString(nameIndex);
                }
                if(pathIndex >= 0){
                    path = c.getString(pathIndex);
                }
                if(albumIndex >= 0){
                    album = c.getString(albumIndex);
                }
                listSong.add(new SongEntity(name, path, album));
                c.moveToNext();
            }
            c.close();
        }
        RecyclerView rv = findViewById(R.id.rv_list_item_song);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new MusicAdapter(listSong, this));
        playPause();
    }

    public void playSong(SongEntity songEntity) {
        index = listSong.indexOf(songEntity);
        this.songEntity = songEntity;

        play();
    }

    /**
     * Sự kiện bấm lên các nút trên thanh điều khiển
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_play) {
            playPause();
        } else if (v.getId() == R.id.iv_next) {
            next();
        } else if (v.getId() == R.id.iv_back) {
            back();
        }
    }

    /**
     * Quay lại bài trước
     */
    private void back() {
        if (index == 0) {
            index = listSong.size() - 1;
        } else {
            index--;
        }

        play();
    }

    /**
     * Chuyển sang bài sau
     */
    private void next() {
        if (index >= listSong.size() - 1 ) {
            index = 0;
        } else {
            index++;
        }

        play();
    }

    /**
     * Chơi hoặc tạm dừng
     */
    private void playPause() {
        if (state == STATE_PLAYING && player.isPlaying()) {
            player.pause();

            ivPlay.setImageLevel(LEVEL_PAUSE);
            state = STATE_PAUSED;
        } else if (state == STATE_PAUSED) {
            player.start();
            state = STATE_PLAYING;
            ivPlay.setImageLevel(LEVEL_PLAY);

        } else {
            play();
        }
    }

    /**
     * Chơi 1 bài hát
     */
    private void play() {
        try {
            songEntity = listSong.get(index);
            tvName.setText(songEntity.getName());
            tvAlbum.setText(songEntity.getAlbum());

            player.reset(); //Chuyển player sang trạng thái chờ
            player.setDataSource(songEntity.getPath()); // Chuyển player sang trạng thái Initialized
            player.prepare(); // Chuyển media sang trang thái Prepared
            player.start(); //Chuyển media sang sang trạng thái started
            ivPlay.setImageLevel(LEVEL_PLAY); //Thiết lập hình ảnh cho nút play_pause
            state = STATE_PLAYING;

            //Lấy ra tổng thòi gian của bài hát đang chơi
            totalTime = getTime(player.getDuration());
            //Thiết lập giá trị lớn nhất cho thanh trạng thái = tổng thời gian của bài hát
            seekBar.setMax(player.getDuration());
            //Chạy vòng lặp để định kỳ cập nhật trạng thái cho thanh trạng thái
            if (thread == null) {
                startLooping();
            }
        } catch (Exception e) {
            Log.i("TAG", " MainActivity play()");
            e.printStackTrace();
            return;
        }
    }

    /**
     * Cập vòng lặp gọi hàm nhật trạng thái trên seekbar
     */
    private void startLooping() {
        thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        return;
                    }
                    runOnUiThread(() -> updateTime());
                    runOnUiThread(() -> autoNext());
                }
            }
        };

        thread.start();
    }

    /**
     * Cập nhật thời gian và thanh trạng thái
     */
    private void updateTime() {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            int time = player.getCurrentPosition();
            tvTime.setText(String.format("%s/%s", getTime(time), totalTime));

            seekBar.setProgress(time);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private String getTime(int time) {
        return new SimpleDateFormat("mm:ss").format(new Date(time));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        thread.interrupt();
    }

    //Các phương thức của thanh trạng thái
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Chơi tiếp khi đã dừng kéo trạng thái
     * @param seekBar
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            player.seekTo(seekBar.getProgress());
        }
    }

    /**
     *  Tự động chuyển bài
     */
    private void autoNext(){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                next();
            }
        });
    }

}