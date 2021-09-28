import datetime
import os
import sys
from inspect import getframeinfo, stack

# Check the environment
if sys.version_info[0] < 3:
    print("Python 3 is required to run the script and not Python "+str(sys.version_info[0]))
    sys.exit(1)

project_home = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)),".."))
file_email = os.path.join(project_home,"email_login.txt")
log_file_name = "default_log_name.txt"
error_log = None

def initialize(log_name):
    global error_log
    global log_file_name
    log_file_name = log_name
    error_log = open(os.path.join(project_home, log_file_name), 'w')
    error_log.write("# BoofCV Regression Cron Log\n")
    error_log.write(datetime.datetime.now().strftime('# %Y %b %d %H:%M\n'))
    error_log.write("Project Directory: "+project_home+"\n")
    error_log.flush()

# Define some commands
def send_email( email_path, message, error_log ):
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
                                    'Subject: %s' % "BoofCV Runtime Regression Fatal Error",
                                    '', message])

                server.sendmail(username+"@gmail.com",[destination],BODY)
                server.quit()
        else:
            error_log.write("email_login.txt not found")
    except Exception as e:
        error_log.write("Failed to email!\n")
        error_log.write("  "+str(e)+"\n")
        error_log.write('  Line {}\n'.format(sys.exc_info()[-1].tb_lineno))


def fatal_error(message, error_log):
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
