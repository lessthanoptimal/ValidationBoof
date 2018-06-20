#!/usr/bin/env python

import math
import optparse
import os
from os import listdir
from os.path import isfile

p = optparse.OptionParser()
p.add_option('--input', '-i', default="frame", help="prefix string")
p.add_option('--output', '-o', default="image", help="prefix string")
p.add_option('--start', '-t', default=0, type="int", help="First number in sequence")
p.add_option('--digits', '-d', default=0, type="int", help="Number of digits in file name. Auto select by default")
p.add_option('--suffix', '-s', default="png", help="suffix of file, typically the file type")

options, arguments = p.parse_args()

input_prefix = options.input
output_prefix = options.output
start_index = options.start
digits = options.digits
img_suffix = options.suffix

print("input          = {:s}".format(input_prefix))
print("output         = {:s}".format(output_prefix))
print("suffix         = {:s}".format(img_suffix))
print("start index    = {:d}".format(start_index))
print("digits         = {:d}".format(digits))

file_list = [f for f in listdir(".") if
             isfile(f) and f.lower().endswith(img_suffix) and f.lower().startswith(input_prefix)]
file_list.sort()

if not file_list:
    p.print_help()
    print( "Found no images...")
    exit(1)

if digits <= 0:
    digits = int(math.log10(len(file_list)))+1

for idx,f in enumerate(file_list):
    new_name = ("{}{:0"+str(digits)+"d}.{}").format(output_prefix,idx+start_index,img_suffix)
    os.rename(f,new_name)

print "Finished"


