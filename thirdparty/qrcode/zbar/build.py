#!/usr/bin/env python3

import os
import sys

script_directory = os.path.dirname(os.path.realpath(__file__))
sys.path.append(os.path.join(script_directory,"../../../scripts"))
from validationboof import *

check_cd(os.path.abspath(script_directory))

# Build the library
check_cd("zbar-0.10")
run_command("find ./zbar -type f -iname '*.[ch]' -print0 | xargs -0 sed -i.bu 's/dprintf/zbarprintf/g'")
run_command("find ./zbar -name '*.bu' -type f -delete")
run_command("./configure 'CXXFLAGS=-O3' --disable-video --without-gtk --without-qt --without-python --without-imagemagick")
run_command("make -j8")

# Build the application
check_cd(os.path.abspath(script_directory))
delete_create("build")
check_cd("build")
run_command("cmake -DCMAKE_BUILD_TYPE=Release ..")
run_command("make -j8")

print("Done building ZBar")