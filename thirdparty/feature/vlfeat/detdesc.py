#!/usr/bin/python
import os
import sys

if len(sys.argv) < 2:
    print 'Need to specify input directory'
    exit()

libname = 'VLFeat_SIFT'

print '------ Reading directory '+sys.argv[1]

for i in range(1,7):
    image_name = sys.argv[1]+"/img"+str(i)+".pgm"
    os.system('./vlfeat-0.9.17/bin/glnxa64/sift -o temp.sift --peak-thresh=6 '+image_name)

#    fin = open('temp.sift','r')
    fout = open(sys.argv[1]+'/DESCRIBE_img'+str(i)+'_'+libname+'.txt','w')

    fout.write('128\n')

    numFound = 0

    with open('temp.sift','r') as fin:
        while True:
            line = fin.readline()
            if not line:
                break
            words = line.strip().split(' ')

            if len(words) != (128+4):
                print 'Crap unexpected word count '+str(len(words))

            [x,y,scale,theta]=words[0:4]
            fout.write("%s %s %s" % (x,y,theta))

            [ fout.write(' '+w) for w in words[4:]]
            fout.write('\n')
            numFound += 1

    print image_name+' found '+str(numFound)

    fin.close()
    fout.close()
