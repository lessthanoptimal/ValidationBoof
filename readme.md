ValidationBoof is a set of utilities for validating the correctness of  algorithms in BoofCV.  This validation is often done by comparing the performance of BoofCV against datasets with ground truth and against external libraries.

website: http://boofcv.org
contact: peter.abeles@gmail.com

Data Files:
https://sourceforge.net/p/validationboof

Source Code:
https://github.com/lessthanoptimal/ValidationBoof

----------------------------------------------

1) Set up e-mail file if desired
2) Create jars for each module
  ./gradlew moduleJars
3) Run regression
  ./gradlew regressionJar
  java -jar regression.jar
4) Copy files from regression/current into regression/baseline
  cp -r regression/current/* regression/baseline
5) Make sure all the tests were run by looking at difference of file list
  diff --brief -r regression/baseline/U8 regression/current/U8

-----------------------------------------------

# E-Mailing of Regression Results

Create a file called "email_login.txt". DO NOT ADD TO GIT.
* First line is your login.
* Second line is your password. 
* Third line is the destination e-mail.

This is configured for gmail only right now. You need to turn on "Allow less secure apps" by going to
https://myaccount.google.com/security