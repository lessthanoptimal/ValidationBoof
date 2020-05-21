ValidationBoof is a set of utilities for validating the correctness of  algorithms in BoofCV.  This validation done by comparing the performance of BoofCV against datasets with ground truth and against external libraries.

* website: [boofcv.org](http://boofcv.org)
* contact: peter.abeles@gmail.com

Data Files:
https://sourceforge.net/p/validationboof

Source Code:
https://github.com/lessthanoptimal/ValidationBoof

# Running Regressions

To run every benchmark do the following. Module jars need to be rebuilt if BoofCV is ever changes.
Look at the command line arguments for regression.jar with the --Help flag to see other options.

1) Set up e-mail file if desired
2) To replicate the exact output use JRE 11.0.1+13-LTS
3) Create jars for each module and build the regression application
```
./gradlew moduleJars;./gradlew regressionJar
```
4) Run regression
```
git clean -fd regression/baseline/
java -jar regression.jar
```
5) Check e-mail for summary of changes from baseline


# Settings Up Results E-Mailing

Create a file called "email_login.txt". DO NOT ADD TO GIT.
* First line is your login.
* Second line is your password. 
* Third line is the destination e-mail.

This is configured for gmail only right now. You need to turn on "Allow less secure apps" by going to
https://myaccount.google.com/security

# Running Periodically

In Linux, to run the regression test periodically you can use the crontab. Open the crontab with "crontab -e"
command then end the line at the bottom. It will run the task at noon on Wednesday and Saturday. Output will
be saved to USER's home directory.

```commandline
00 12 * * 3,6 export PATH="/opt/jdk/latest/bin:$PATH";/usr/bin/python3 /home/USER/projects/ValidationBoof/scripts/cronscript.py > /home/USER/cron_output.log 2>&\
1
```
