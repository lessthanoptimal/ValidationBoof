#!/usr/bin/env python

import optparse
import os
from os import listdir
from os.path import isfile, join
from shutil import copyfile

p = optparse.OptionParser()
p.add_option('--input', '-i', default=".")
p.add_option('--output', '-o', default="output")
p.add_option('--skip', '-s', default=10)
p.add_option('--keepname', '-k', action="store_false")


options, arguments = p.parse_args()

dir_input = options.input
dir_output = options.output
skip = int(options.skip)

print "Skip        {}".format(options.skip)
print "Rename      {}".format(not options.keepname)

# Create output directory if it doesn't exist
if not os.path.exists(dir_output):
    os.makedirs(dir_output)

file_list = [f for f in listdir(dir_input) if isfile(join(dir_input, f))]
file_list.sort()

print "Total files in {}".format(len(file_list))

count = 0
for f in file_list:
    count += 1
    if (count-1) % skip != 0:
        continue

    dst = f
    suffix = f.split(".")[1]
    if not options.keepname:
        dst = "image{:04d}.{:s}".format(count/skip,suffix)
    copyfile(join(dir_input,f), join(dir_output,dst))

