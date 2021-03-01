#!/usr/bin/env python3

import os
import urllib.request
from subprocess import call

home_directory = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))

def run_command(command):
    if os.system(command):
        caller = getframeinfo(stack()[1][0])
        caller_info = "File: %s Line: %d" % (caller.filename, caller.lineno)
        fatal_error("\n  Failed to execute '"+command+"'\n  "+caller_info+"\n")

def check_cd(path):
    try:
        os.chdir(path)
    except:
        fatal_error("Failed to cd into '"+path+"'")

# Make sure it's in the same directory as the script so that all the paths are correct
os.chdir(home_directory)

if not os.path.isdir("data"):
    os.mkdir("data")
check_cd("data")

# Extract all the datasets
call(["unzip","../ukbench.zip"])

for i in range(0,10):
    call(["unzip","-o","../mirflickr1m.v3/images{}.zip".format(i)])

print("Done")
