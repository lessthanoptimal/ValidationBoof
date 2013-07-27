#!/usr/bin/python
import os
import sys

inputDir = "../../data/track_rect/TLD"

if len(sys.argv) >= 2:
    inputDir = sys.argv[1]

libname = 'copentld'

print '------ Reading directory '+inputDir

datasets = ["01_david","02_jumping","03_pedestrian1","04_pedestrian2",
            "05_pedestrian3","06_car","07_motocross","08_volkswagen","09_carchase","10_panda"]

datasets = ["08_volkswagen"]

def convertData( dataset ):
    fout = open(libname+'_'+dataset+'.txt','w')
    
    with open(libname+'_'+dataset+'_raw.txt') as f:
        content = f.readlines()

    for s in content:
        rect = [float(x) for x in s.split(' ') ]
        fout.write(str(rect[1])+","+str(rect[2])+","+str(rect[1]+rect[3])+","+str(rect[2]+rect[4])+"\n")
    fout.close()

def checkExist( filename ):
    try:
        with open(filename): pass
    except IOError:
        return False
    return True

for dataset in datasets:
    fullpath = inputDir+"/"+dataset
    rectstr = open(fullpath+'/init.txt', 'r').read()
    rect = [int(x) for x in rectstr.split(',') ]

    if checkExist(fullpath+'/00001.jpg'):
        imageType = 'jpg'
    else:
        imageType = 'png'

    # convert to x,y,width,height
    rectstr = str(rect[0])+","+str(rect[1])+","+str(rect[2]-rect[0])+","+str(rect[3]-rect[1])
    command = 'opentld -d IMGS -i '+fullpath+'/%05d.'+imageType+' -b '+rectstr+' -p '+libname+'_'+dataset+'_raw.txt'
    print command
    os.system(command)

    convertData(dataset)
