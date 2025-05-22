from drum import *
from bass import *

colours = ["C","C", "Am", "Am","F","F", "G","G"]
number=len(colours)

speed=500000
brokenchord=1#1打开分解和弦 0关闭分解和弦
guitar=1#1打开吉他 0关闭即为钢琴
##########钢琴的接口
guitar=0
brokenchord=1
filename='piano.mid'
createOneChord(colours,speed,filename,brokenchord,guitar)

##########吉他的接口
guitar=1
brokenchord=1
filename='guitar.mid'
createOneChord(colours,speed,filename,brokenchord,guitar)


#############鼓的接口
drum(number,speed)
#number数量用于drum输入


############bass的接口
createbass(colours,number,speed)







   # str1 = str(str00)

#createOneChord(str,50000)

from chengxusheji.integration import MelodyGenerator

if __name__ == "__main__":
    generator = MelodyGenerator(config_path="hmm_config.json")
    melody = generator.generate("input.csv", emotion=0.8)
    print(melody)