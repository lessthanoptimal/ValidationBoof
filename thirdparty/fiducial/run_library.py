#!/usr/bin/env python

import os
import subprocess
import sys
from os.path import isdir, join

if len(sys.argv) < 3:
    print "arguments: <config file> <output directory>"
    exit(0)

def skip_comments( f ):
    line = f.readline()
    while line:
        if line[0] is '#':
            line = f.readline()
        else:
            return line.replace('\n', '')
    return line

def parseConfigFile( file_name ):
    try:
        output = None
        with open(file_name, 'r') as f:
            path = skip_comments(f)
            transform = ""
            for i in range(3):
                transform += skip_comments(f) + "\n"
            output = [path,transform]
        f.close()
        return output
    except EnvironmentError:
        raise RuntimeError( "Failed to open "+file_name+" for output")

def saveTransform( file_name , transform ):
    try:
        with open(file_name, 'w') as f:
            f.write(transform)
        f.close()
    except EnvironmentError:
        raise RuntimeError( "Failed to open "+file_name+" for output")   

path_config = sys.argv[1]
path_data = "../../data/fiducials/image/"
path_output = sys.argv[2]

scenarios = [ f for f in os.listdir(path_data) if isdir(join(path_data,f))]

[path_bin,transform] = parseConfigFile( path_config )

print "loaded config:"
print "  bin = '"+path_bin+"'"

if not os.path.exists(path_output):
    os.makedirs(path_output)

saveTransform(join(path_output,"libToStandard.txt"),transform)

for scenario in scenarios:
    input = join(path_data,scenario)
    output = join(path_output,scenario)
    os.makedirs(output)

    subprocess.call([path_bin,input,output])


