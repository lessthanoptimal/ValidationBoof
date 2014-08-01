#!/usr/bin/python

import os
from os import listdir
from subprocess import call

os.mkdir('2009')
os.chdir('2009')

fileList = [ f for f in listdir("..") if f.endswith('.rar') ]
for f in fileList:
    call(['unrar','e','../'+f])

fileList = [ f for f in listdir(".") if f.endswith('.tiff') ]
for f in fileList:
    call(['convert',f,f[0:3]+'_truth.bmp'])
