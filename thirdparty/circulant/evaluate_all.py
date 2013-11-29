#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

videos = ["cliffbar","coke11","david","dollar","faceocc","faceocc2","girl","surfer","sylv","tiger1","tiger2","twinings"]

for v in videos:
    os.system("python/evaluation.py --gui=False --save=True -i ../../data/track_rect/MILTrack/"+v)
