#!/usr/bin/env python3

import os
import shutil
import sys

script_directory = os.path.dirname(os.path.realpath(__file__))
sys.path.append(os.path.join(script_directory,"../../../scripts"))
from validationboof import *

check_cd(os.path.abspath(script_directory))

# Clean up previous build and rebuild it
shutil.rmtree("build")
os.makedirs("build")
check_cd("build")
run_command("cmake ..")
run_command("make -j8")

print("Done building Quirc")