#!/usr/bin/env python

import shutil
import sys
from os import listdir, makedirs
from os.path import isfile, join

if len(sys.argv) != 3:
    print "Need to specify input directory and output directory"
    exit(1)


input_dir = sys.argv[1]
output_dir = sys.argv[2]

shutil.rmtree(output_dir)
makedirs(output_dir)


l = listdir(input_dir)
l.sort()

skip = 4
count = 0

print "Total files {}".format(len(l))

for f in l:
    if not isfile(join(input_dir,f)):
        continue

    # print "count {} mod {}".format(count,count%skip)
    if count%skip == 0:
        suffix = f.split(".")[1]
        dst = join(output_dir,"image{:04d}.{:s}".format(count/skip,suffix))
        shutil.copyfile(join(input_dir,f),dst)
    count += 1

print "Done!"




