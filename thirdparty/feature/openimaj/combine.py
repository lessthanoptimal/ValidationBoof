#!/usr/bin/python

import fnmatch
import os

print 'Working so far'

matches = []
for root, dirnames, filenames in os.walk('.'):
  for filename in fnmatch.filter(filenames, '*SNAPSHOT.jar'):
      matches.append(os.path.join(root, filename))

os.system('mkdir tmp')
os.chdir('tmp')
os.system('touch foo.txt')

for m in matches:
    os.system('unzip -n ../'+m)

os.system('rm -rf META-INF')

os.system('jar cf all.jar *')

