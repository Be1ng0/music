from flask import Flask, send_file, request
from midi2audio import FluidSynth
import os
import tempfile

app = Flask(__name__)

# 配置 SoundFont 路径
SOUNDFONT_PATH = os.path.join(app.static_folder, 'soundfonts/GeneralUser-GS')
MIDI_FOLDER = os.path.join(app.root_path, 'static/midi')
MP3_FOLDER = os.path.join(app.root_path, 'mp3')

# 创建目录（如果不存在）
os.makedirs(MIDI_FOLDER, exist_ok=True)
os.makedirs(MP3_FOLDER, exist_ok=True)

# 初始化 FluidSynth
fs = FluidSynth(sound_font=SOUNDFONT_PATH)


@app.route('/convert', methods=['GET'])
def convert_midi_to_mp3():
    # 获取 MIDI 文件名称（从请求参数）
    midi_filename = request.args.get('filename', 'final.mid')
    midi_path = os.path.join(MIDI_FOLDER, midi_filename)

    # 检查 MIDI 文件是否存在
    if not os.path.exists(midi_path):
        return {"error": f"MIDI 文件不存在: {midi_path}"}, 404

    try:
        # 创建 MP3 文件路径
        mp3_filename = midi_filename.replace('.mid', '.mp3')
        mp3_path = os.path.join(MP3_FOLDER, mp3_filename)

        # 使用 FluidSynth 将 MIDI 转换为 MP3
        fs.midi_to_audio(midi_path, mp3_path)

        # 返回 MP3 文件
        return send_file(
            mp3_path,
            mimetype='audio/mp3',
            as_attachment=True,
            download_name=mp3_filename
        )
    except Exception as e:
        return {"error": f"转换失败: {str(e)}"}, 500


@app.route('/upload', methods=['POST'])
def upload_midi():
    # 处理 MIDI 文件上传（可选）
    if 'midi_file' not in request.files:
        return {"error": "未找到文件"}, 400

    file = request.files['midi_file']
    if file.filename == '':
        return {"error": "未选择文件"}, 400

    if file and file.filename.endswith('.mid'):
        filename = 'uploaded.mid'
        file_path = os.path.join(MIDI_FOLDER, filename)
        file.save(file_path)
        return {"message": "上传成功", "filename": filename}

    return {"error": "无效的文件格式"}, 400


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)