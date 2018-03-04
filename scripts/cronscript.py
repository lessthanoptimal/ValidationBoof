#!/usr/bin/env python3

# This is a script that's intended to be run on a regular basis by a cron task
# It will rebuild all dependencies and then run the regression
# Don't run if you care about any unsaved results. it cleans a bunch of crap out

log_file_name = "cronlog.txt"

import datetime
import os
import sys

# Check the environment
if sys.version_info[0] < 3:
    print("Python 3 is required to run the script and not Python "+str(sys.version_info[0]))
    sys.exit(1)

project_home = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)),".."))

error_log = open(os.path.join(project_home,log_file_name), 'w')
error_log.write("# Validation Boof Cron Log\n")
error_log.write(datetime.datetime.now().strftime('# %Y %b %d %H:%M\n'))
error_log.write("Project Directory: "+project_home+"\n")
error_log.flush()

file_email = os.path.join(project_home,"email_login.txt")

# Define some commands
def send_email( message ):
    try:
        import smtplib

        if os.path.isfile(file_email):
            with open(file_email) as f:
                username = f.readline().rstrip()
                password = f.readline().rstrip()
                destination = f.readline().rstrip()

                server = smtplib.SMTP_SSL('smtp.gmail.com', 465)
                server.login(username+"@gmail.com", password)

                BODY = '\r\n'.join(['To: %s' % destination,
                                    'From: %s' % (username+"@gmail.com"),
                                    'Subject: %s' % "Validation Boof Fatal Error",
                                    '', message])

                server.sendmail(username+"@gmail.com",[destination],BODY)
                server.quit()
        else:
            error_log.write("email_login.txt not found")
    except Exception as e:
        error_log.write("Failed to email!\n")
        error_log.write("  "+str(e)+"\n")
        error_log.write('  Line {}\n'.format(sys.exc_info()[-1].tb_lineno))



def fatal_error(message):
    if len(sys.exc_info()) > 0:
        error_log.write('  Error below from line {}\n'.format(sys.exc_info()[-1].tb_lineno))
    send_email(message)
    error_log.write(message+"\n")
    error_log.write("\n\nFATAL ERROR\n\n")
    error_log.close()
    sys.exit(1)

def run_command(command):
    if os.system(command):
        fatal_error("Failed to execute '"+command+"'")

def check_cd(path):
    try:
        os.chdir(path)
    except:
        fatal_error("Failed to cd into '"+path+"'")

# EJML is a special case
if os.path.isdir(os.path.join(project_home,"..","ejml")):
    check_cd(os.path.join(project_home,"..","ejml"))
    error_log.write("Building {}\n","ejml")
    run_command("git checkout SNAPSHOT")
    run_command("git pull")
    run_command("git clean -f")
    run_command("./gradlew autogenerate")
    run_command("./gradlew install")
else:
    error_log.write("Skipping {} directory does not exist\n".format("ejml"))

# List of projects with standard build
project_list = ["ddogleg","georegression","boofcv"]

for p in project_list:
    path_to_p = os.path.join(project_home,"..",p)
    if not os.path.isdir(path_to_p):
        error_log.write("Skipping {} directory does not exist\n".format(p))
        continue
    error_log.write("Building {}\n".format(p))
    error_log.flush()
    check_cd(path_to_p)
    run_command("git checkout SNAPSHOT")
    run_command("git pull")
    run_command("./gradlew assemble")
    run_command("./gradlew install")

# Now it's time to build
error_log.write("Building regression\n")
error_log.flush()
check_cd(project_home)
run_command("git checkout SNAPSHOT")
run_command("git pull")
# run_command("git clean -f") <-- can'tdo this becauze it will zap the email_login.txt file!
run_command("./gradlew clean")
run_command("./gradlew moduleJars")
run_command("./gradlew regressionJar")
error_log.write("Starting regression\n")
error_log.flush()
run_command("java -jar regression.jar")

error_log.write("\n\nFinished Script\n\n")
error_log.close()

print("Done!")
