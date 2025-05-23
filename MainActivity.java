package com.example.interface3;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.Tool.Function.AudioFunction;
import com.Tool.Function.CommonFunction;
import com.Tool.Function.FileFunction;
import com.Tool.Function.LogFunction;
import com.Tool.Function.VoiceFunction;
import com.Tool.Global.Constant;
import com.Tool.Global.Variable;
//import com.czt.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import zty.composeaudio.Tool.Interface.ComposeAudioInterface;
import zty.composeaudio.Tool.Interface.VoicePlayerInterface;
import zty.composeaudio.Tool.Player.VoicePlayerEngine;

//在java\com\tool\global\constant里面改采样率那三个参数
//在initdata（）里面写文件地址

public class MainActivity extends AppCompatActivity {

    private Button btnBack;
    private Button btnCompose;
    private Button btnStop;

    private SeekBar skbOffset;

    private VerticalSeekBar vsbVoice;
    private VerticalSeekBar vsbBgm;

    private String voicePcmUrl;
    private String bgmPcmUrl;
    private String composeVoiceUrl;

    private TextView tvVoice;
    private TextView tvBgm;
    private TextView tvOffset;

    private int voiceWeight;
    private int bgmWeight;
    private int Offset;

    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
            init(R.layout.activity_main);
        //初始化

    }

    private void init(int layoutid) {
        setContentView(layoutid);
        bindView();

        //初始化数据地址与按键功能
        initData();

        instance=this;

    }

    private void bindView() {
        btnBack = findViewById(R.id.btnBack);
        btnCompose = findViewById(R.id.btnCompose);
        btnStop = findViewById(R.id.btnStop);
        skbOffset = findViewById(R.id.skbOffset);
        vsbVoice = findViewById(R.id.vsbVoice);
        vsbBgm = findViewById(R.id.vsbBgm);
        tvVoice= findViewById(R.id.tv1);
        tvBgm=findViewById(R.id.tv2);
        tvOffset=findViewById(R.id.tv3);
    }

//    初始化数据地址与按键功能
    public void initData() {

        btnStop.setEnabled(false);

        //播放/暂停按键
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FileFunction.IsFileExists(composeVoiceUrl)) {
                    VoiceFunction.PlayToggleVoice(composeVoiceUrl, (VoicePlayerInterface)instance);
                }
            }
        });
        //返回界面一按键
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //使用intent进行界面跳转,跳转至界面1，Interface1
                Intent intent = new Intent(MainActivity.this,Interface1.class);
                startActivity(intent);

            }
        });
        //合成歌曲按键
        btnCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                compose();
                btnStop.setEnabled(true);
            }
        });
        //人声音量调节按键
        vsbVoice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvVoice.setText(Integer.toString(i/10));
                voiceWeight=i/10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //伴奏音量调节按键
        vsbBgm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvBgm.setText(Integer.toString(i/10));
                bgmWeight=i/10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //人声对齐按键
        skbOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvOffset.setText(Integer.toString(i-5));
                Offset=i-5;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //录音pcm文件和伴奏pcm文件的储存地址，最后是生成的MP3文件的储存地址
        File guitar = new File(Environment.getExternalStorageDirectory(), "guitar.pcm");
        voicePcmUrl = guitar.getPath();

        File drum = new File(Environment.getExternalStorageDirectory(), "drum.pcm");

        bgmPcmUrl = drum.getPath();
        File composed = new File(Environment.getExternalStorageDirectory(), "new.mp3");

        composeVoiceUrl = composed.getPath();

    }

    private void compose() {

        btnCompose.setEnabled(false);

        AudioFunction.BeginComposeAudio(voicePcmUrl, bgmPcmUrl, composeVoiceUrl, false,
                voiceWeight, bgmWeight,
                Offset * Constant.RecordDataNumberInOneSecond);
    }

}

