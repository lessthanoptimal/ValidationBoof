# Platforms Tests on

* Ubuntu 16.04 Desktop
* Raspbian on a Raspberry PI 3 B+
* ODROID UX4

# Initial Set Up

First download the test data. The most mindless way to do that is using the provided python script:

```
cd ValidationBoof/data/fiducials
./download.py
```

The build process is semi-automated. The first time you build this benchmark you need to go into each project directory and follow the readme's instructions. Then run the build.py script. On some platforms you might need to install additional packages. If you do, make a note and submit a pull request so others don't want to do the same. If there's no build script you can assume there's nothing special you need to do and move on.

Example:
```
cd ValidationBoof/thirdparty/qrcode/zbar
cat readme.md
"Oh I need to manually download the zbar source code! This nice script patches the code for me. I like this script"
```

Recommended Order:
- Install/Build OpenCV 3.3.1
- quirc
- zbar
- Make sure Java JDK is installed. 1.8 or newer
- BoofCV and ZXing will work the first time

If you want to rebuild everything later on you can simply run the ./build_all_libraries.py script.

# Running the Benchmark

- pip install shapely
- ./build_all_libraries.py
- ./detect_all_libraries.py
- ./evaluate_results_count.py
- ./evaluate_results_labeled.py

# Convert output for web

```
convert -density 150 -shave 120x0 detection_categories.pdf  detection_categories.png
convert -density 150 detection_summary.pdf detection_summary.png
convert -density 150 runtime_summary.pdf runtime_summary.png
convert -density 150 -shave 120x0 runtime_categories_relative_mean.pdf runtime_categories_relative_mean.png
convert -density 150 category_counts.pdf category_counts.png
```