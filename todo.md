- Create a history of SHAs for use in future binary search
- Add support for --Quick which will only run very fast tests for change detection
  * Need to create "quick" datasets
- Command Line
  * Specify U8 or F32 only
- New Regressions
  * Visual odometry - RGB-D
  * Visual odometry - Mono-Plane
  * template matching regression
  * homography - read up on how others have done this
  * video stabilization
- Object Trackers: Very fast way to detect changes
  * Have a simple change detection regression so that the point at which results changed can be easily localized