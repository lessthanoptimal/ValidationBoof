#!/usr/bin/env python3

if __name__ == '__main__':
    import os
    import sys

    script_directory = os.path.dirname(os.path.realpath(__file__))
    sys.path.append(os.path.join(script_directory,"../../../scripts"))
    from validationboof import *

    check_cd(os.path.abspath(script_directory))

    # See if the jar exists. If not build it
    run_command("./gradlew clean")
    run_command("./gradlew ZXingAztecCodeJar")

    print("Done building ZXing")