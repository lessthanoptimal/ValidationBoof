Description of changes to benchmarks and justifications for change in performance from previous versions.

## Version 0.41
2022/

- Added Aztec Code regression
- DetectPolygonRegression changed values due to slight change in how relative contour thresholds are computed
- QR Code
  * Fixed issue with low version QR Codes where it was rejecting them where it shouldn't. Improved results.
  * Added more images to test set and added F-Score to metrics to make local changes easier to see
- ThreeViewReconstruction
  * Algorithm got updated to have entire self calibration pipeline in RANSAC
- Added Multi Camera Calibration regression

## Version 0.38 - 0.40
2022/1/17

- Tooling
  * Automated checks for runtime performance
  * Format summary e-mail with HTML and uses fixed sized font
- Ransac
  * Anything that used RANSAC and similar had its random seed changed. This is due to its internal
    implementation being modified to enable concurrency
- ObjectTrackingRegression
  * Sparse Flow's speed is computed differently to take in account it not processing images after losing track
- TrifocalTensorFRegression
  * Was incorrectly implemented before and has better metrics now
- New Regression
  * EvaluateEpipolarScore3DFRegression
  * Sparse and Dense Reconstruction with planar regions
- Results
  * QR Code has been updated after improving local lighting invariance to decoding

## Version 0.37
2020/12/21

- Improved runtime metrics across all regression and put into a common format to make automatic checking possible
- Added more runtime regressions and compacted others into a single file
- config_local.yaml to specify local benchmark behavior and information
- Moved runtime regressions into a separate directory that is machine specific
- Add CPU info to MachineInfo.txt
- PointTrackers
  * There was a mistake (for a long time) where reset was not called at the start of a new trial
- Corners
  * Needed to update ground truth list because the original BoofCV code was buggy when there were multiple sets
    Only maximums were saved because the factory created the wrong extractor. Even if that wasn't there, it would have
    not returned the correct number of features.
- Three View Metric Scene
  * Re-tuned and is much faster, but a little less stable
  * Couldn't tune to get exactly the same results as before, but the speed had degraded too much
  * Suspicious recent changes to feature selection result in slightly worse selection or redundant information
- Visual Odometry
  * DualPnP and StereoDepth changed due to a bug fix in PruneCloseTracks 
    9605be22ae62e7095ad5ba04ca512003eb72f915
- Calibration
  * Results changed insignificantly when DDogleg removed O(N^2) matrix multiplication on 2020/OCT/1
- Disparity
  * Results degraded slightly due to a bug fix
  * The issue was that disparity range was being returned as max-min instead of the actual range. 
    This lead to the allowed disparity values being clipped. Effectively the last (and can't be subpixel)
    value was discarded
  * This should act as a hint on increasing reliability of results
- Dense Feature SIFT F32
  * Metrics show it 2x slower, when re-running old version it matched the new slower results.
  * The faster result appear in multiple times but could not reproduce now even with old code/Java
  * Worth noting that some old results also show the slower time.

## Version 0.36
2020/May/18

- Prunes empty ERRORLOG files after running regression
- Modified summary to include java version and dirty flag info
- Fixed long-standing issue of multi band descriptors crashing when they encounter a single band image
- Fixed encoding/decoding issue in QR regression
- Explanation of Differences
  * Modified Image.isInside(float,float) and that resulted in small change in results for object tracking and VO 
  * Fiducial runtime is computed at same time as other metrics
  * Fiducial max error is now image size dependent
  * Stereo Visual Odometry: All changed to include bundle adjustment plus other modifications
  * Quad Stereo VO has a runtime regression because association using image location was removed, but was 
    compensated for by switching from SURF to BRIEF
  * Three-View Reconstruction: Change in how the rectified view was computed improved image fill
  * Disparity changed because NCC had how it handled texture error modified with a new score function
  * BRIEF produces slightly different results because of Image.isInside() change.
  * DDA trackers produce different results because of a bug fix involving PointTrack

## Version 0.35
2019/Dec/23

- Revamp Chessboard detector
- Added DetectLineRegression
- Added DisparityRegression
- Circulant's results changed slightly due to improvements in mean-shift
- Stereo Depth's change is likely a result of how the disparity search changed
  * Didn't pin point the exact change and adjusted it to be similar
- Three View Stereo
  * Uses Census for error now
  * Adjusts rectified image's shape based on rotation
  * Increased RANSAC iterations to improve stability
- Dense SIFT F32 for two test runs it was slower, last and final run it was about the same
- SFM got slower for one test case. Replicated with 0.34. JVM change?
- Overall it seems like most stuff is running slower for unknown reasons

## Version 0.34
2019/07/07

- Default SIFT scale-space changed. No longer doubled.
- Fiducials
  * Fixed bug in regression where total wrong order was not being reset
  * Turned off wrong order check for chessboard

## Version 0.33
2019/03/19

- DDA Point Tracker was improved. Now prunes tracks if there are too many. On average accuracy improved by a good bit.
- QR Code threshold was tuned some more improving overall performance. Slight performance drop in rare edge cases.
- QR Code more images added to regression
- Calibration optimization method was changed. Results of for practical purposes identical
- Turned off concurrency by default for all regression tests to ensure results are deterministic

## Version 0.32

- Added three view reconstruction performance metrics
- Several fiducial algorithms were not properly undistorting the image
  when a lens distortion model was provided. This has been fixed.
  As a result some of the runtime tests have slowed down a little bit.

## Version 0.31
2018/10/16

- Bundle Adjustment Regression
  * Reduction in residual score 50% and 95%
- PNP Regression
  * Planar P4P
- Trifocal Tensor Regression
_ Visual Odometry
  * QuadPnP has gotten slower. Not sure why. Regression was fixed earlier dealing with score.

## Version 0.30
2018/05/20

- Change in calibration results due to change in how they are computed. Within floating point tolerance
- Polyline based fiducials
  * Slight change in performance caused by a fix in the new polyline algorithm
  * Large improvement in speed caused by new contour algorithm
- Chessboard
  * Fixed issue with very close targets
  * Caused a slight decrease in performance with other targets
- FAST
  * Algorithm has been changed to be more faithful
  * Bright and dark features are in two different lists
- Point Tracker
  * Can handle multiple sets of tracks
  * minimums and maximums are handled separately
  

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


