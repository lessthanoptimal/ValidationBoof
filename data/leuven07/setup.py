#!/usr/bin/python

import sys
import os
from subprocess import call

call(["unzip","leuven07.zip"])
os.rename("leuven07/left","left")
os.rename("leuven07/right","right")
call(["rm","-rf","leuven07"])
