#!/usr/bin/env python3

# This is a script that's intended to be run on a regular basis by a cron task
# It will rebuild all dependencies and then run the regression
# Don't run if you care about any unsaved results. it cleans a bunch of crap out


import os

import tools
from tools import *

# Check the environment
initialize("cronlog_boofcv_jmh.txt")
error_log = tools.error_log

# Paths need to be absolute since they are passed to the code, which is relative to that repo
email_path = os.path.abspath(os.path.join(project_home, "email_login.txt"))
regression_path = os.path.abspath(os.path.join(project_home, "runtime_regression"))
local_settings_path = os.path.abspath(os.path.join(project_home, "settings_local.yaml"))

print("regression_path", regression_path)

error_log.write("Start Runtime Regression\n")
error_log.flush()
check_cd("../../boofcv")
run_command("git clean -fd")
run_command("git fetch")
run_command("git checkout SNAPSHOT")
run_command("git reset --hard origin/SNAPSHOT")
run_command("./gradlew clean")
run_command("./gradlew autogenerate")
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