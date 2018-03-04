#!/usr/bin/env python

import math
import optparse
import os
from os import listdir
from os.path import isfile

p = optparse.OptionParser()
p.add_option('--input', '-i', default="frame")
p.add_option('--output', '-o', default="image")
p.add_option('--suffix', '-s', default="png")

options, arguments = p.parse_args()

input_prefix = options.input
output_prefix = options.output
img_suffix = options.suffix


file_list = [f for f in listdir(".") if
             isfile(f) and f.lower().endswith(img_suffix) and f.lower().startswith(input_prefix)]
file_list.sort()

if not file_list:
    p.print_help()
    print( "Found no images...")
    exit(1)

digits = int(math.log10(len(file_list)))+1

for idx,f in enumerate(file_list):
    new_name = ("{}{:0"+str(digits)+"d}.{}").format(output_prefix,idx,img_suffix)
    os.rename(f,new_name)

print "Finished"


