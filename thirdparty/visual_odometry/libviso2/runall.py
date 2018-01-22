#!/usr/bin/python
import os

# Run all the training scenarios
for i in range(0,12):
    seqDir = "../../data/KITTI/sequences/"
    seqNum = "%02d" % i

    os.system('./a.out '+seqDir+" "+seqNum)

    
