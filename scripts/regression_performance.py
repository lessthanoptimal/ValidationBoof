#!/usr/bin/env python3

# This is a script that's intended to be run on a regular basis by a cron task
# It will rebuild all dependencies and then run the regression
# Don't run if you care about any unsaved results. it cleans a bunch of crap out

import os

import tools
from tools import *

initialize("cronlog_performance.txt")
error_log = tools.error_log

# List of dependencies to build
project_list = [{"name":"ejml","autogen":True},
                {"name":"ddogleg","autogen":False},
                {"name":"georegression","autogen":True},
                {"name":"boofcv","autogen":True}]

for lib in project_list:
    p = lib["name"]
    path_to_p = os.path.join(project_home, "..", p)
    if not os.path.isdir(path_to_p):
        error_log.write("Skipping {} directory does not exist\n".format(p))
        error_log.flush()
        continue
    error_log.write("Building {}\n".format(p))
    error_log.flush()
    check_cd(path_to_p)
    run_command("git clean -fd")            # Remove all untracked files to avoid stale auto generated code
    run_command("git checkout SNAPSHOT")
    run_command("git fetch")
    run_command("git reset --hard origin/SNAPSHOT") # Won't fail if some idiot rewrote git history
    run_command("git submodule update")
    if lib["autogen"]:
        run_command("./gradlew autogenerate")
    run_command("./gradlew clean")
    run_command("./gradlew PublishToMavenLocal")

# Now it's time to build
error_log.write("Building regression\n")
error_log.flush()
check_cd(project_home)
run_command("git checkout SNAPSHOT")
run_command("git fetch")
run_command("git reset --hard origin/SNAPSHOT")
# run_command("git clean -f") <-- can't do this because it will zap the email_login.txt file!
run_command("./gradlew clean")
run_command("./gradlew moduleJars")
run_command("./gradlew regressionJar")
error_log.write("Starting regression\n")
error_log.flush()
run_command("java -jar regression.jar")

error_log.write("\n\nFinished Script\n\n")
error_log.close()

print("Done!")
