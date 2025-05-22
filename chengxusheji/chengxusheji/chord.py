import random
class chord:

 def __init__(self, note0, velocityType,chordType):



    self.velocityType=velocityType
    self.note0=note0
    self.note=[0,0,0]
    self.note[0]=note0
    self.note[1]=0
    self.note[2]=0
    self.velocity=[0,0,0]



    if chordType=="bigChord":
     self.bigChordNote(note0)
    elif chordType=="smallChord":
     self.smallChordNote(note0)
    elif chordType=="addChordNote":
     self.addChordNote(note0)
    elif chordType=="susChordNote":
     self.susChordNote(note0)
    else: self.decreaseChordNote(note0)


    if velocityType==0:
        self.firstChordVelocity()
    elif velocityType==1:
        self.secondChordVelocity()
    elif velocityType==2:
        self.thirdChordVelocity()
    elif velocityType==3:
        self.fourthChordVelocity()
    return


 def bigChordNote(self,note0):
        self.note[1] = note0 + 4
        self.note[2] = self.note[1] + 3
        return

 def smallChordNote(self,note0):
        self.note[1] = note0 + 3
        self.note[2] = self.note[1] + 4
        return

 def addChordNote(self,note0):
        self.note[1] = note0 + 4
        self.note[2]= self.note[1] + 4
        return

 def decreaseChordNote(self, note0):
        self.note[1] = note0 + 3
        self.note[2] = self.note[1] + 3
        return
 def susChordNote(self,note0):
        self.note[1] = note0 + 2
        self.note[2] = self.note[1] + 5

 def firstChordVelocity(self): #velocityType 0
     self.velocity[0]=90+random.randint(0,8)
     self.velocity[1]=80+random.randint(0,8)
     self.velocity[2]=70+random.randint(0,8)
     return
 def secondChordVelocity(self): #velocityType 0
     self.velocity[0]=60+random.randint(0,8)
     self.velocity[1]=55+random.randint(0,8)
     self.velocity[2]=45+random.randint(0,8)
     return
 def thirdChordVelocity(self): #velocityType 0
     self.velocity[0]=85+random.randint(0,8)
     self.velocity[1]=78+random.randint(0,8)
     self.velocity[2]=65+random.randint(0,8)
     return
 def fourthChordVelocity(self): #velocityType 0
     self.velocity[0]=56+random.randint(0,8)
     self.velocity[1]=46+random.randint(0,8)
     self.velocity[2]=43+random.randint(0,8)
     return