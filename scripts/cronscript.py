#!/usr/bin/env python3

import os
import sys

from tools import *

# Change directory to make it easy to find the scripts
os.chdir(os.path.dirname(os.path.realpath(__file__)))


# Attempts to run the script and if anything fails, print out what failed and email if possible
def run_script(script_name):
    if not os.system("python3 " + script_name):
        return
    sys.stderr.print("Failed to run " + script_name)
    send_email("email_login.txt", "Failed: " + script_name, sys.stderr)


run_script("regression_performance.py")
run_script("regression_jmh.py")

print("Master Cronscript Done!")
