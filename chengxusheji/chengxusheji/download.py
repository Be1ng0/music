# -*- coding: UTF-8 -*-
from flask import send_file, send_from_directory,Flask
import os

app = Flask(__name__)#创建一个服务，赋值给APP
@app.route("/download/<filename>", methods=['GET'])
def download_file(filename):
    # 需要知道2个参数, 第1个参数是本地目录的path, 第2个参数是文件名(带扩展名)
    directory = os.getcwd()  # 假设在当前目录
    return send_from_directory(directory, filename, as_attachment=True)
if __name__ == '__main__':
  app.run(host='0.0.0.0',port=5001)