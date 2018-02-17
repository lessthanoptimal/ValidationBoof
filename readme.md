ValidationBoof is a set of utilities for validating the correctness of  algorithms in BoofCV.  This validation is often done by comparing the performance of BoofCV against datasets with ground truth and against external libraries.

* website: [boofcv.org](http://boofcv.org)
* contact: peter.abeles@gmail.com

Data Files:
https://sourceforge.net/p/validationboof

Source Code:
https://github.com/lessthanoptimal/ValidationBoof

# Running Regressions

To run every benchmark do the following. Module jars need to be rebuilt if BoofCV is every changes.
Look at the command line arguments for regression.jar with the --Help flag to see other options.

1) Set up e-mail file if desired
2) Create jars for each module
```
./gradlew moduleJars
```
3) Run regression
```
./gradlew regressionJar
java -jar regression.jar
```
4) Check e-mail for summary of changes from baseline


# Settings Up Results E-Mailing

Create a file called "email_login.txt". DO NOT ADD TO GIT.
* First line is your login.
* Second line is your password. 
* Third line is the destination e-mail.

This is configured for gmail only right now. You need to turn on "Allow less secure apps" by going to
https://myaccount.google.com/security