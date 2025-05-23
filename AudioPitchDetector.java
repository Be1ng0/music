package com.example.interface3;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import java.util.ArrayList;
import java.util.List;

public class AudioPitchDetector {
    private static final int SAMPLE_RATE = 44100; // 提高采样率以获取更准确的高频
    private static final int BUFFER_SIZE = 2048; // 增加缓冲区大小提高低频检测
    private static final int OVERLAP = 1024; // 添加重叠以提高时间分辨率
    private static final float MIN_FREQ = 60.0f; // 降低最低频率阈值
    private static final float MAX_FREQ = 1500.0f; // 提高最高频率阈值
    private static final float PROBABILITY_THRESHOLD = 0.6f; // 降低概率阈值
    private static final int PITCH_BUFFER_SIZE = 3; // 减少平滑缓冲区以提高响应速度
    private static final int MIN_NOTE_DURATION = 3; // 最小持续帧数

    private AudioDispatcher dispatcher;
    private StringBuilder noteSequence;
    private OnPitchDetectedListener listener;
    private List<Integer> pitchBuffer;
    private int currentNote = -1;
    private int noteDuration = 0;
    private int frameCount = 0;
    private int framesPerBeat;

    public interface OnPitchDetectedListener {
        void onPitchDetected(String noteSequence);
    }

    public AudioPitchDetector(int bpm, OnPitchDetectedListener listener) {
        this.listener = listener;
        this.noteSequence = new StringBuilder();
        this.pitchBuffer = new ArrayList<>(PITCH_BUFFER_SIZE);
        this.framesPerBeat = (int)((60.0f / bpm) * SAMPLE_RATE / BUFFER_SIZE);
    }

    public void startDetection() {
        noteSequence.setLength(0);
        pitchBuffer.clear();
        currentNote = -1;
        noteDuration = 0;
        frameCount = 0;

        // 创建音频调度器
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, OVERLAP);

        // 创建音高处理器
        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float pitchInHz = result.getPitch();
                final float probability = result.getProbability();

                // 在后台线程处理音高，避免UI卡顿
                processPitch(pitchInHz, probability);

                // 跟踪节拍和小节
                frameCount++;
                if (frameCount % (framesPerBeat * 4) == 0) {
                    // 每4拍添加一个小节分隔符
                    if (noteSequence.length() > 0 && !noteSequence.toString().endsWith("/")) {
                        noteSequence.append("/");
                    }
                }
            }
        };

        // 创建并初始化pitchProcessor变量
        AudioProcessor pitchProcessor = new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                SAMPLE_RATE,
                BUFFER_SIZE,
                pdh
        );

        // 添加音频处理器
        dispatcher.addAudioProcessor(pitchProcessor);

        // 启动音频处理线程
        Thread audioThread = new Thread(dispatcher, "Audio Thread");
        audioThread.start();
    }

    public void stopDetection() {
        if (dispatcher != null && !dispatcher.isStopped()) {
            dispatcher.stop();
            dispatcher = null;

            // 处理最后一个音符
            if (currentNote != -1 && noteDuration >= MIN_NOTE_DURATION) {
                appendNote(currentNote);
            }

            // 清理序列
            String finalSequence = cleanNoteSequence(noteSequence.toString());
            if (listener != null) {
                listener.onPitchDetected(finalSequence);
            }
        }
    }

    private void processPitch(float pitchInHz, float probability) {
        // 过滤无效频率和低置信度结果
        if (pitchInHz < MIN_FREQ || pitchInHz > MAX_FREQ || probability < PROBABILITY_THRESHOLD) {
            handleSilence();
            return;
        }

        int note = hzToNumber(pitchInHz);

        // 应用移动平均平滑
        pitchBuffer.add(note);
        if (pitchBuffer.size() > PITCH_BUFFER_SIZE) {
            pitchBuffer.remove(0);
        }

        int smoothedNote = getMostFrequentPitch(pitchBuffer);

        // 处理音符变化
        if (smoothedNote == currentNote) {
            noteDuration++;
        } else {
            if (currentNote != -1 && noteDuration >= MIN_NOTE_DURATION) {
                appendNote(currentNote);
            }
            currentNote = smoothedNote;
            noteDuration = 1;
        }
    }

    private void handleSilence() {
        // 如果之前有音符，且持续时间足够长，则记录它
        if (currentNote != -1 && noteDuration >= MIN_NOTE_DURATION) {
            appendNote(currentNote);
            currentNote = -1;
            noteDuration = 0;
        }
    }

    private void appendNote(int note) {
        // 添加到序列
        if (noteSequence.length() > 0 && !noteSequence.toString().endsWith(",") && !noteSequence.toString().endsWith("/")) {
            noteSequence.append(",");
        }
        noteSequence.append(note);
    }

    private String cleanNoteSequence(String sequence) {
        // 移除开头的分隔符
        if (sequence.startsWith(",")) {
            sequence = sequence.substring(1);
        }
        if (sequence.startsWith("/")) {
            sequence = sequence.substring(1);
        }

        // 确保结尾没有多余的分隔符
        if (sequence.endsWith(",")) {
            sequence = sequence.substring(0, sequence.length() - 1);
        }
        if (sequence.endsWith("/")) {
            sequence = sequence.substring(0, sequence.length() - 1);
        }

        return sequence;
    }

    private int hzToNumber(double hz) {
        double midi = 12 * (Math.log(hz / 440) / Math.log(2)) + 69;
        int noteNum = (int) Math.round(midi);
        return noteNum % 12; // 返回0-11之间的音符编号
    }

    private int getMostFrequentPitch(List<Integer> pitchList) {
        // 简单的众数算法，找到最频繁出现的音高
        int[] count = new int[12]; // 只需要0-11的音符编号
        int maxCount = 0;
        int mostFrequent = -1;

        for (int pitch : pitchList) {
            count[pitch]++;
            if (count[pitch] > maxCount) {
                maxCount = count[pitch];
                mostFrequent = pitch;
            }
        }

        return mostFrequent;
    }
}