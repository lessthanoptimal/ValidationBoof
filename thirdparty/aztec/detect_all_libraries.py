#!/usr/bin/env python3

import optparse
import os
import shutil
import sys
import time
from os.path import join

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../scripts"))

from validationboof import *

# Handle command line options
p = optparse.OptionParser()
p.add_option('--Input', '-i', default="data",help="Location of directory with input images")
p.add_option('--Library', '-l', default=None,help="Specificy a specific library to run. Default ALL")

options, arguments = p.parse_args()

dir_input = options.Input
target = options.Library

timing = {}

if target is not None:
    if os.path.isdir(join(project_home,target)):
        output_path = os.path.join("results",target)
        if os.path.exists(output_path):
            shutil.rmtree(output_path)
        os.makedirs(output_path)
        script_path = join(project_home,join(target,"detect.py"))
        run_command("python3 "+script_path+" -i "+dir_input+" -o "+os.path.abspath(output_path))
    else:
        print(target+" is not a directory")
else:
    # Go through each directory and see if it has a script to
    for d in os.listdir():
        if not os.path.isdir(join(project_home,d)):
            continue
        if d is "results":
            continue

        script_path = join(project_home,join(d,"detect.py"))

        if not os.path.isfile(script_path):
            # print("skipping "+script_path)
            continue
        print("Running Script "+script_path)

        output_path = os.path.join("results",d)
        if os.path.exists(output_path):
            shutil.rmtree(output_path)
        os.makedirs(output_path)

        time0 = time.time()
        run_command("python3 "+script_path+" -i "+os.path.abspath(dir_input)+" -o "+os.path.abspath(output_path))
        time1 = time.time()
        timing[d] = time1-time0

print("Total Processing Time")
for s in sorted(timing.keys()):
    print("  {} {}".format(s,timing[s]))

print("Finished! Without errors...")