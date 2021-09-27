#!/usr/bin/env python3

# This is a script that's intended to be run on a regular basis by a cron task
# It will rebuild all dependencies and then run the regression
# Don't run if you care about any unsaved results. it cleans a bunch of crap out

log_file_name = "cronlog_boofcv_jmh.txt"

import datetime
import os
import sys
from inspect import getframeinfo, stack

# Check the environment
if sys.version_info[0] < 3:
    print("Python 3 is required to run the script and not Python "+str(sys.version_info[0]))
    sys.exit(1)

project_home = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))

error_log = open(os.path.join(project_home,log_file_name), 'w')
error_log.write("# BoofCV JMH Regression Cron Log\n")
error_log.write(datetime.datetime.now().strftime('# %Y %b %d %H:%M\n'))
error_log.write("Project Directory: "+project_home+"\n")
error_log.flush()

# Paths need to be absolute since they are passed to EJML code, which is relative to that repo
email_path = os.path.abspath(os.path.join(project_home, "../email_login.txt"))
regression_path = os.path.abspath(os.path.join(project_home, "../runtime_regression"))
local_settings_path = os.path.abspath(os.path.join(project_home, "../local_settings.yaml"))

print("regression_path", regression_path)

# Define some commands
def send_email( message ):
    try:
        import smtplib

        if os.path.isfile(email_path):
            with open(email_path) as f:
                username = f.readline().rstrip()
                password = f.readline().rstrip()
                destination = f.readline().rstrip()

                server = smtplib.SMTP_SSL('smtp.gmail.com', 465)
                server.login(username+"@gmail.com", password)

                BODY = '\r\n'.join(['To: %s' % destination,
                                    'From: %s' % (username+"@gmail.com"),
                                    'Subject: %s' % "EJML Regression Fatal Error",
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
    error_log.write(message+"\n")
    error_log.write("\n\nFATAL ERROR\n\n")
    error_log.close()

    # Read in the log file and send it
    with open(os.path.join(project_home,log_file_name), 'r') as f:
        email_txt=f.read()

    send_email(email_txt)
    sys.exit(1)


def run_command(command):
    if os.system(command):
        caller = getframeinfo(stack()[1][0])
        caller_info = "File: %s Line: %d" % (caller.filename, caller.lineno)
        fatal_error("\n  Failed to execute '"+command+"'\n  "+caller_info+"\n")


def check_cd(path):
    try:
        os.chdir(path)
    except:
        fatal_error("Failed to cd into '"+path+"'")


error_log.write("Start Runtime Regression\n")
error_log.flush()
check_cd("../../boofcv")
run_command("./gradlew run --console=plain runtimeRegression -Dexec.args="
            "\"--EmailPath {} --LocalSettingsPath {} --ResultsPath {}\"".
            format(email_path,local_settings_path, regression_path))
error_log.write("Pulling latest regression code\n")
error_log.flush()
check_cd(project_home)
# Since the latest regression code could change this script it should be run outside of this script
# before this script is invoked
run_command("git pull")
error_log.write("\n\nFinished Script\n\n")
error_log.close()

print("Done!")