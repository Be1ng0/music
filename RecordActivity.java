package com.example.musicapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class RecordActivity extends AppCompatActivity {
    // 节拍器
    private float tempo;
    private int section;
    private int pp;
    private Handler handler;
    private Timer mytimer;
    private TextView result;
    private TextView pitchDisplay;

    // 录制
    private Button btn_start, btn_end;
    private AudioPitchDetector pitchDetector;
    private StringBuilder noteSequenceBuilder = new StringBuilder();
    private EditText pitchInput; // 新增：音高序列输入框

    String instru;
    float happy;
    String style; // 新增：风格参数

    // 播放
    private Button btn_play, btn_pause;
    private boolean isRelease = true;
    private MediaPlayer mPlayer = null;

    private Button btn_generate;
    Handler mhandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            btn_play.setEnabled(true);
            btn_download.setEnabled(true);
            updateButtonStyle(btn_play);
            updateButtonStyle(btn_download);
        }
    };

    // 下载
    private Button btn_download;
    Context context = this;

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createCustomLayout()); // 使用自定义布局

        // 请求存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        Bundle myBundle = this.getIntent().getExtras();
        tempo = myBundle.getInt("tempo");
        section = myBundle.getInt("section");
        happy = (float) myBundle.getInt("happy") / 100;
        instru = myBundle.getString("instru");
        style = myBundle.getString("style"); // 新增：获取风格参数

        // 节拍器
        pp = 1;
        handler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        result.setText(pp + "");
                        if (pp != section) {
                            pp++;
                        } else {
                            pp = 1;
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };

        // 初始化优化后的音高检测器
        pitchDetector = new AudioPitchDetector((int) tempo, new AudioPitchDetector.OnPitchDetectedListener() {
            @Override
            public void onPitchDetected(String sequence) {
                Log.d("hcc", "检测到的音高: " + sequence);
                // 累积音高序列，不立即处理
                if (noteSequenceBuilder.length() > 0) {
                    noteSequenceBuilder.append("/");
                }
                noteSequenceBuilder.append(sequence);

                runOnUiThread(() -> {
                    pitchDisplay.setText("检测到的音高序列:\n" + noteSequenceBuilder.toString());
                    pitchInput.setText(noteSequenceBuilder.toString());
                    updateGenerateButtonState();
                });
            }
        });

        // 录制按钮事件
        btn_start.setOnClickListener(v -> {
            addButtonClickEffect(v);
            // 清空之前的序列
            noteSequenceBuilder.setLength(0);
            pitchInput.setText("");
            pitchDetector.startDetection();
            mytimer = new Timer();
            float tempFloat = 60 / tempo * 1000;
            mytimer.schedule(new MyTimerTask(), 0, (long) tempFloat);
            pp = 1;
            btn_end.setEnabled(true);
            btn_start.setEnabled(false);
            pitchInput.setEnabled(false);
            updateButtonStyle(btn_start);
            updateButtonStyle(btn_end);
        });

        btn_end.setOnClickListener(v -> {
            addButtonClickEffect(v);
            pitchDetector.stopDetection();
            mytimer.cancel();
            btn_end.setEnabled(false);
            btn_start.setEnabled(true);
            pitchInput.setEnabled(true);
            updateButtonStyle(btn_start);
            updateButtonStyle(btn_end);

            // 不再在这里传参
            Log.d("hcc", "录制结束，等待生成按钮点击");
            updateGenerateButtonState();
        });

        // 为输入框添加文本变化监听器
        pitchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateGenerateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 播放控制
        btn_play.setOnClickListener(v -> {
            addButtonClickEffect(v);
            if (isRelease) {
                String url = "http://10.122.217.245:5001/download/final.mid";
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                isRelease = false;
                try {
                    mPlayer.setDataSource(url);
                    mPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mPlayer.start();
            btn_play.setEnabled(false);
            btn_pause.setEnabled(true);
            updateButtonStyle(btn_play);
            updateButtonStyle(btn_pause);
        });

        btn_pause.setOnClickListener(v -> {
            addButtonClickEffect(v);
            mPlayer.pause();
            btn_play.setEnabled(true);
            btn_pause.setEnabled(false);
            updateButtonStyle(btn_play);
            updateButtonStyle(btn_pause);
        });

        // 生成伴奏 - 现在在这里整理并发送参数
        btn_generate.setOnClickListener(v -> {
            addButtonClickEffect(v);
            String inputSequence = pitchInput.getText().toString().trim();
            if (!inputSequence.isEmpty()) {
                // 整理参数
                String[] sections = inputSequence.split("/");
                StringBuilder finalSequence = new StringBuilder();

                for (String section : sections) {
                    String[] notes = section.split(",");
                    if (notes.length > 0) {
                        if (finalSequence.length() > 0) {
                            finalSequence.append("/");
                        }
                        finalSequence.append(notes[0]);
                        for (int i = 1; i < notes.length; i++) {
                            finalSequence.append(",").append(notes[i]);
                        }
                    }
                }

                // 发送到服务器
                post_notes(finalSequence.toString(), style); // 新增：传递风格参数
                mhandler.sendEmptyMessageDelayed(1, 3000);
            } else {
                Toast.makeText(context, "请输入音高序列或先录制音频", Toast.LENGTH_SHORT).show();
            }
        });

        // 下载功能
        btn_download.setOnClickListener(v -> {
            addButtonClickEffect(v);
            downloadFile2();
        });

        // 初始化生成按钮状态
        updateGenerateButtonState();
    }

    // 添加按钮点击效果
    private void addButtonClickEffect(View view) {
        Animation animation = new AlphaAnimation(1.0f, 0.7f);
        animation.setDuration(150);
        animation.setRepeatCount(0);
        animation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(animation);
    }

    // 更新生成按钮状态
    private void updateGenerateButtonState() {
        String inputSequence = pitchInput.getText().toString().trim();
        btn_generate.setEnabled(!inputSequence.isEmpty());
        updateButtonStyle(btn_generate);
    }

    // 更新按钮样式
    private void updateButtonStyle(Button button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(12);

        if (button.isEnabled()) {
            drawable.setColor(Color.parseColor("#616161")); // 深灰色（可点击）
            button.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(Color.parseColor("#E0E0E0")); // 浅灰色（不可点击）
            button.setTextColor(Color.parseColor("#9E9E9E"));
        }

        button.setBackground(drawable);
    }

    // 创建自定义布局
    private View createCustomLayout() {
        // 主滚动视图
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // 主线性布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // 节拍器显示
        TextView title1 = new TextView(this);
        title1.setText("节拍器");
        title1.setTextSize(20);
        title1.setGravity(Gravity.CENTER);
        title1.setPadding(0, 16, 0, 16);
        title1.setTextColor(Color.BLACK);
        mainLayout.addView(title1);

        result = new TextView(this);
        result.setText("1");
        result.setTextSize(48);
        result.setGravity(Gravity.CENTER);
        result.setPadding(0, 24, 0, 24);
        result.setBackgroundColor(Color.LTGRAY);
        result.setWidth(120);
        result.setHeight(120);
        result.setMaxWidth(120);
        result.setMaxHeight(120);
        result.setMinWidth(120);
        result.setMinHeight(120);
        mainLayout.addView(result);

        // 录制控制
        TextView title2 = new TextView(this);
        title2.setText("音频录制");
        title2.setTextSize(20);
        title2.setGravity(Gravity.CENTER);
        title2.setPadding(0, 32, 0, 16);
        title2.setTextColor(Color.BLACK);
        mainLayout.addView(title2);

        LinearLayout recordLayout = new LinearLayout(this);
        recordLayout.setOrientation(LinearLayout.HORIZONTAL);
        recordLayout.setGravity(Gravity.CENTER);
        recordLayout.setPadding(0, 16, 0, 16);
        recordLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        btn_start = new Button(this);
        btn_start.setText("开始录制");
        btn_start.setTextSize(16);
        btn_start.setPadding(24, 12, 24, 12);
        btn_start.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        recordLayout.addView(btn_start);
        updateButtonStyle(btn_start);

        btn_end = new Button(this);
        btn_end.setText("结束录制");
        btn_end.setTextSize(16);
        btn_end.setPadding(24, 12, 24, 12);
        btn_end.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btn_end.setEnabled(false);
        recordLayout.addView(btn_end);
        updateButtonStyle(btn_end);

        mainLayout.addView(recordLayout);

        // 音高序列显示
        pitchDisplay = new TextView(this);
        pitchDisplay.setText("检测到的音高序列将显示在这里");
        pitchDisplay.setTextSize(14);
        pitchDisplay.setPadding(16, 16, 16, 16);
        pitchDisplay.setBackgroundColor(Color.LTGRAY);
        pitchDisplay.setMaxLines(5);
        pitchDisplay.setEllipsize(android.text.TextUtils.TruncateAt.END);
        pitchDisplay.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.addView(pitchDisplay);

        // 音高序列输入
        TextView title3 = new TextView(this);
        title3.setText("音高序列输入");
        title3.setTextSize(20);
        title3.setGravity(Gravity.CENTER);
        title3.setPadding(0, 32, 0, 16);
        title3.setTextColor(Color.BLACK);
        mainLayout.addView(title3);

        pitchInput = new EditText(this);
        pitchInput.setHint("例如: 6,6,7,6/4,5,4,2/3,3,4,3/1,2,1,0");
        pitchInput.setTextSize(16);
        pitchInput.setPadding(16, 16, 16, 16);
        pitchInput.setMinLines(3);
        pitchInput.setMaxLines(5);
        pitchInput.setGravity(Gravity.TOP);
        pitchInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.addView(pitchInput);

        // 生成和播放控制
        TextView title4 = new TextView(this);
        title4.setText("生成与播放");
        title4.setTextSize(20);
        title4.setGravity(Gravity.CENTER);
        title4.setPadding(0, 32, 0, 16);
        title4.setTextColor(Color.BLACK);
        mainLayout.addView(title4);

        btn_generate = new Button(this);
        btn_generate.setText("生成伴奏");
        btn_generate.setTextSize(18);
        btn_generate.setPadding(24, 16, 24, 16);
        btn_generate.setEnabled(false);
        mainLayout.addView(btn_generate);
        updateButtonStyle(btn_generate);

        LinearLayout playLayout = new LinearLayout(this);
        playLayout.setOrientation(LinearLayout.HORIZONTAL);
        playLayout.setGravity(Gravity.CENTER);
        playLayout.setPadding(0, 16, 0, 16);
        playLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        btn_play = new Button(this);
        btn_play.setText("播放");
        btn_play.setTextSize(16);
        btn_play.setPadding(24, 12, 24, 12);
        btn_play.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btn_play.setEnabled(false);
        playLayout.addView(btn_play);
        updateButtonStyle(btn_play);

        btn_pause = new Button(this);
        btn_pause.setText("暂停");
        btn_pause.setTextSize(16);
        btn_pause.setPadding(24, 12, 24, 12);
        btn_pause.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btn_pause.setEnabled(false);
        playLayout.addView(btn_pause);
        updateButtonStyle(btn_pause);

        mainLayout.addView(playLayout);

        // 下载按钮
        btn_download = new Button(this);
        btn_download.setText("下载伴奏");
        btn_download.setTextSize(16);
        btn_download.setPadding(24, 12, 24, 12);
        btn_download.setEnabled(false);
        mainLayout.addView(btn_download);
        updateButtonStyle(btn_download);

        // 添加所有视图到滚动视图
        scrollView.addView(mainLayout);
        return scrollView;
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法下载文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void post_notes(final String notes, String style) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            StringBuilder host = new StringBuilder();
            host.append("http://10.122.217.245:5000/main?str=");
            host.append(notes);
            // key
            host.append("&a=C&speed=");
            host.append((int) tempo);
            host.append("&c=").append(instru).append("&happy=").append(happy);
            host.append("&style=").append(style); // 新增：添加风格参数

            Log.i("hcccc", "上传的host" + host);

            try {
                URL url = new URL(host.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                Log.d("hcc", "connected");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String str = reader.readLine();
                Log.d("hcc", "reuturn:" + str);

                reader.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.d("hcc", "finally");
                if (connection != null)
                    connection.disconnect();
            }
        }).start();
    }

    private void downloadFile2() {
        new Thread(() -> {
            try {
                // 创建 MediaStore 文件条目
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "final.mid");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/midi");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);

                ContentResolver resolver = getContentResolver();
                Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                Uri uri = resolver.insert(contentUri, contentValues);

                if (uri == null) {
                    runOnUiThread(() -> Toast.makeText(context, "创建文件失败", Toast.LENGTH_LONG).show());
                    return;
                }

                // 通过 HttpURLConnection 下载文件
                URL url = new URL("http://10.122.217.245:5001/download/final.mid");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(context, "服务器错误", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 将下载内容写入 MediaStore
                try (InputStream input = connection.getInputStream();
                     OutputStream output = resolver.openOutputStream(uri, "w")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(context, "下载完成", Toast.LENGTH_SHORT).show();
                    btn_play.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e("DownloadError", "下载失败", e);
                runOnUiThread(() -> Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mytimer != null) {
            mytimer.cancel();
        }
    }
}}