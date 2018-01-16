ValidationBoof is a set of utilities for validating the correctness of  algorithms in BoofCV.  This validation is often done by comparing the performance of BoofCV against datasets with ground truth and against external libraries.

Data Files:
https://sourceforge.net/p/validationboof

Source Code:
https://github.com/lessthanoptimal/ValidationBoof

----------------------------------------------

1) Run regression
2) Copy files from regression/current into regression/baseline
  cp -r regression/current/* regression/baseline
3) Make sure all the tests were run by looking at difference of file list
  diff --brief -r regression/baseline/U8 regression/current/U8
4)


-----------------------------------------------

website: http://boofcv.org
contact: peter.abeles@gmail.com
