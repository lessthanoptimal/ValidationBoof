## ARma

http://xanthippi.ceid.upatras.gr/people/evangelidis/arma/

Original code has been modified so it can actually compile, cmake script, and a few other improvements.

## Calibration

OpenCV 2.4

1) Build with examples turned on
2) cpp-example-imagelist_creator list.yaml *.jpg
3) cpp-example-calibration -w 7 -h 5 -s 3 -o camera.yml list.yaml

RMS error reported by calibrateCamera: 0.299495
Calibration succeeded. avg reprojection error = 0.30

4) calibration is stored in camera.yml