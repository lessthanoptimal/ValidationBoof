Description of changes to benchmarks and justifications for change in performance from previous versions.

## Version 0.28

- Polyline Algorthm was changed in square based fidudicals
  * In general this improved speed and reliability
- Chessboard Calibration
  * Failing with large images when chessboard goes outside the border due to shapes being very concave
  * Made threshold local block size larger to compensate
  * Performance in non-uniform lighting dropped as a result
- Thresholding for all other calibration targets
  * Switched to block based techniques for speed
  * Made threshold block size adaptive based on image size
  * Large boost to speed and mixed results in other metrics but slightly worse.
  * Circle Regular's performance plummeted for blurred images
- CornerDetectorChangeRegression results have changed because a bug was fixed
  * The passed in corner config class was being modified leading to undefined behavior the next time it was used 
  * This same fix also modified the results for DualPNP Visual Odometry
  
TODO Change threshold for fast in fiducial to OTSU


## Version 0.27
* Change in RANSAC implementation has caused several results to change.
** Affected algorithms: Visual Odometry
** This change does not change the algoritm only the implementation and the numbers it draws.
** It's suspected that results on the whole got worse because algorithms were over tuned. Getting worse was dumb luck.
* Slight change in fiducial solutions
** Not sure. It's within floating point tolerance and is assumed to be a minor change in tuning


