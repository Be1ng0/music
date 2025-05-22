from testpython import *

def createbass(colours,number,speed):
 mid = mido.MidiFile()
 track = mido.MidiTrack()
 mid.tracks.append(track)
 track.append(mido.Message('program_change', program=33, time=0))  # 这个音轨使用的乐器
 track.append(mido.MetaMessage('set_tempo', tempo=speed, time=0))
 num = 1
 firstchord=1

 for colour in colours:

    str1 = str(colour)
    str1ChordNote0 = chordNote0(str1)
    print(str1ChordNote0)
    if (num == 1):
        if (firstchord == 1):
            track.append(mido.Message('note_on', note=str1ChordNote0-24, velocity=96, time=0))
            track.append(mido.Message('note_off', note=str1ChordNote0-24, velocity=96, time=120))
            num += 1
            firstchord = 0
        else:
            track.append(mido.Message('note_on', note=str1ChordNote0-24, velocity=96, time=600))
            track.append(mido.Message('note_off', note=str1ChordNote0-24, velocity=96, time=120))
            num += 1
    elif (num == 2):
        num += 1
    elif (num == 3):
        track.append(mido.Message('note_on', note=str1ChordNote0-24, velocity=96, time=720))
        track.append(mido.Message('note_off', note=str1ChordNote0-24, velocity=96, time=120))
        num += 1

    elif (num == 4):
        track.append(mido.Message('note_on', note=str1ChordNote0-24, velocity=96, time=0))
        track.append(mido.Message('note_off', note=str1ChordNote0-24, velocity=96, time=360))
        num = 1



 mid.save('贝斯.mid')
 mid.save('final.mid')