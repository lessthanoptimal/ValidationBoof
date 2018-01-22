#!/usr/bin/python

import os
from os import listdir
from os.path import isfile, join
from subprocess import call

fileList = [ f for f in listdir(".") if isfile(join(".",f)) and f.endswith('.zip') ]

for f in fileList:
    name = f.strip('.zip')
    os.mkdir(name)
    os.chdir(name)
    call(["unzip","../"+f])
    os.chdir('..')

os.rename('twinnings','twinings')
