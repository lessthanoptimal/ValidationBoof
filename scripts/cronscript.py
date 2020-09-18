#!/usr/bin/env python3

# This is a script that's intended to be run on a regular basis by a cron task
# It will rebuild all dependencies and then run the regression
# Don't run if you care about any unsaved results. it cleans a bunch of crap out

log_file_name = "cronlog.txt"

import datetime
import os
import sys
from inspect import getframeinfo, stack

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
                                    'Subject: %s' % "ValidationBoof Fatal Error",
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
        fatal_error("Failed to execute '"+command+"'\n"+caller_info+"\n")

def check_cd(path):
    try:
        os.chdir(path)
    except:
        fatal_error("Failed to cd into '"+path+"'")


# List of dependencies to build
project_list = [{"name":"ejml","autogen":True},
                {"name":"ddogleg","autogen":False},
                {"name":"georegression","autogen":True},
                {"name":"boofcv","autogen":True}]

for lib in project_list:
    p = lib["name"]
    path_to_p = os.path.join(project_home,"..",p)
    if not os.path.isdir(path_to_p):
        error_log.write("Skipping {} directory does not exist\n".format(p))
        error_log.flush()
        continue
    error_log.write("Building {}\n".format(p))
    error_log.flush()
    check_cd(path_to_p)
    run_command("git clean -f")            # Remove all untracked files to avoid stale auto generated code
    run_command("git checkout SNAPSHOT")
    run_command("git pull")
    run_command("git submodule update")
    if lib["autogen"]:
        run_command("./gradlew autogenerate")
    run_command("./gradlew clean")
    if p == "boofcv":
        run_command("./gradlew PublishToMavenLocal")
    else:
        run_command("./gradlew install")

# Now it's time to build
error_log.write("Building regression\n")
error_log.flush()
check_cd(project_home)
run_command("git checkout SNAPSHOT")
run_command("git pull")
# run_command("git clean -f") <-- can'tdo this because it will zap the email_login.txt file!
run_command("./gradlew clean")
run_command("./gradlew moduleJars")
run_command("./gradlew regressionJar")
error_log.write("Starting regression\n")
error_log.flush()
run_command("java -jar regression.jar")

error_log.write("\n\nFinished Script\n\n")
error_log.close()

print("Done!")
