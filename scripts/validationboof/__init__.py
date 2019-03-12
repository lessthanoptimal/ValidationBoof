import os
import shutil
import sys

if sys.version_info[0] < 3:
    print("Python 3 is required to run the script and not Python "+str(sys.version_info[0]))
    sys.exit(1)

def fatal_error(message):
    sys.stderr.write(message+"\n")
    sys.stderr.write("\n\nFAILED!!! LOOK AT MESSAGES ABOVE\n\n")
    sys.exit(1)

def run_command(command):
    if os.system(command):
        fatal_error("Failed to execute '"+command+"'")

def check_cd(path):
    if os.chdir(path):
        fatal_error("Failed to cd into '"+path+"'")

def delete_create(path):
    if os.path.exists(path):
        shutil.rmtree(path)
    os.makedirs(path)