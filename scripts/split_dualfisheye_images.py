#!/usr/bin/env python

import optparse
import os
from PIL import Image
from os import listdir
from os.path import isfile, join

p = optparse.OptionParser()
p.add_option('--input', '-i', default=".")
p.add_option('--output', '-o', default="output")
p.add_option('--suffix', '-s', default="jpg")


options, arguments = p.parse_args()

dir_input = options.input
dir_output = options.output
img_suffix = options.suffix


# Create output directory if it doesn't exist
if not os.path.exists(dir_output):
    print "Creating output directory"
    os.makedirs(dir_output)

file_list = [f for f in listdir(dir_input) if isfile(join(dir_input, f)) and f.lower().endswith(img_suffix)]
file_list.sort()

if not file_list:
    print "Found no images..."

for idx,f in enumerate(file_list):
    if idx%10 == 0:
        print "Processing {} out of {}".format(idx,len(file_list))
    im = Image.open(join(dir_input,f))
    w,h = im.size
    left = im.crop((0,0,w/2,h))
    right = im.crop((w/2,0,w,h))

    try:
        left.save(join(dir_output,"left_"+f[:-4]+".png"))
        right.save(join(dir_output,"right_"+f[:-4]+".png"))
    except IOError:
        print("cannot convert", infile)

print "Finished"


