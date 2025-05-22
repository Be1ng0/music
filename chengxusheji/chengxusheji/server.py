from flask import Flask, jsonify, request, send_file, send_from_directory
import os
import dataProcess
import xlwt
import hmm
from hmm import HybridModel  # 导入 HybridModel 类
import testpython
import drum
import bass

app = Flask(__name__)


@app.route('/')
@app.route('/main', methods=['post', 'get'])
def main():
    workbook = xlwt.Workbook(encoding='utf-8')
    booksheet = workbook.add_sheet('Sheet 1', cell_overwrite_ok=True)

    s = request.args.get('str')
    a = request.args.get('a')
    choose = request.args.get('c')
    speed = request.args.get('speed')
    happy = request.args.get('happy')

    #
    ma = dataProcess.input(s, a)
    [h, l] = ma.shape
    for i in range(h):
        for j in range(l):
            booksheet.write(i, j, ma[i, j])
    workbook.save('melody.xls')
    # core = "hmm.exe"
    # r_v = os.system(core)
    # return str(r_v)
    strma = ["C", "#C", "D", "#D", "E", "F", "#F", "G", "#G", "A", "#A", "B"]
    colours = []
    # 暂时注释掉调用 mmm 函数的代码
    # ma = hmm.mmm(float(happy))

    # 这里可以根据实际需求实现一个临时的 ma 列表
    ma = [0, 1, 2, 3, 4]  # 临时示例

    for i in ma:
        if i <= 11:
            colours.append(strma[i])
        elif i <= 23:
            colours.append(strma[i - 12] + "m")
        elif i <= 35:
            colours.append(strma[i - 24] + "aug")
        elif i <= 47:
            colours.append(strma[i - 36] + "im")
        else:
            colours.append(strma[i - 48] + "sus")

    filename = '1.mid'
    # colours = ["#C", "Am", "Dm", "Em", "Fsus"]
    if (int(choose[0]) == 1):
        testpython.createOneChord(colours, int(str(speed)), '钢琴分解.mid', 1, 0)
    if (int(choose[1]) == 1):
        testpython.createOneChord(colours, int(speed), '钢琴柱式.mid', 0, 0)
    if (int(choose[2]) == 1):
        testpython.createOneChord(colours, int(speed), '吉他分解.mid', 1, 1)
    if (int(choose[3]) == 1):
        testpython.createOneChord(colours, int(speed), '吉他柱式.mid', 0, 1)
    if (int(choose[4]) == 1):
        drum.drum(len(colours), int(speed))
    if (int(choose[5]) == 1):
        bass.createbass(colours, len(colours), int(speed))
    return str(1)


app.run(host='0.0.0.0', port=5000, debug=True)