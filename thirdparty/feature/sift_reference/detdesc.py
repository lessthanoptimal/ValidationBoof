#!/usr/bin/python
import os
import sys

if len(sys.argv) < 2:
    print 'Need to specify input directory'
    exit()

libname = 'SIFT_REFERENCE'

print '------ Reading directory '+sys.argv[1]

for i in range(1,7):
    image_name = sys.argv[1]+"/img"+str(i)+"\.pgm"
    os.system('./siftDemoV4/sift < '+image_name+" > temp.txt")

    fin = open('temp.txt','r')
    fout = open(sys.argv[1]+'/DESCRIBE_img'+str(i)+'_'+libname+'.txt','w')

    # Read the number of features in the file
    N = int(fin.readline().split(' ')[0])

    fout.write('128\n')

    for j in range(N):
        desc = [ float(w) for w in fin.readline()[:-1].split(' ') ]

        [y,x,scale,theta]=desc[0:4]
        fout.write("%7.3f %7.3f %7.5f" % (float(x),float(y),float(theta)))

        for descLine in range(0,7):
            [ fout.write(' '+w) for w in fin.readline()[:-1].split(' ') if w.isdigit() ]
        fout.write('\n')

    fin.close()
    fout.close()