Description of changes to benchmarks and justifications for change in performance from previous versions.

## Version 0.30
2018/

- Change in calibration results due to change in how they are computed. Within floating point tolerance

TODO whitelist file

## Version 0.29
2018/03/20

- Added regression for background models
- Added more threshold algorithms to text thresholding regression
- Broke regression up into different code modules. This was does so that when performing a binary search for a 
  regression broken code due to a change in API in an unrelated part of the code won't break the whole test
- Python cronscript which pulls latest code, builds dependencies, builds project and runs project
  * errors are caught and e-mailed
  * final report is also e-mailed
- Fidual
  * Pose estimate changed by a small amount. Within floating point precision
  * QuadePoseEstimator no longer converting coordinates from norm->pixel->norm just norm->pixel
- Polygon
  * Switched code from old algorithm to the new one now that it can handle non-looping contours
  * Changed some results by an insignificant amount
  * Minor change in new algorithm's split to fix an edge code probably causes a minor change too

## Version 0.28
2018/01/20

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
  * Other regressions affected:
    - DualPNP Visual Odometry
- Change in DDogleg's LeastMedianOfSquares
  * It now computes the median by rounding instead of flooring
  * Sparse Flow Object Tracker and TLD object changes had their results changed

## Version 0.27
* Change in RANSAC implementation has caused several results to change.
** Affected algorithms: Visual Odometry
** This change does not change the algoritm only the implementation and the numbers it draws.
** It's suspected that results on the whole got worse because algorithms were over tuned. Getting worse was dumb luck.
* Slight change in fiducial solutions
** Not sure. It's within floating point tolerance and is assumed to be a minor change in tuning


