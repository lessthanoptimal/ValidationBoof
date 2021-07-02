#!/usr/bin/env python3

import urllib.request
from subprocess import call
import os
from os import listdir

files = ["DIBC02009_Test_images-handwritten.rar", "DIBCO2009-GT-Test-images_handwritten.rar",
         "DIBCO2009-GT-Test-images_printed.rar", "DIBCO2009_Test_images-printed.rar"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/segmentation/"+f
    urllib.request.urlretrieve(url,f)

os.mkdir('2009')
os.chdir('2009')

fileList = [ f for f in listdir("..") if f.endswith('.rar') ]
for f in fileList:
    call(['unrar','e','../'+f])

fileList = [ f for f in listdir(".") if f.endswith('.tiff') ]
for f in fileList:
    call(['convert',f,f[0:3]+'_truth.bmp'])
