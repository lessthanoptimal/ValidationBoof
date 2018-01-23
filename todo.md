		// TODO Visual odometry - RGB-D
		// TODO Visual odometry - Mono-Plane
		// TODO template matching regression

- At the top of all results indicate which regression test generated it
- All accuracy metrics be prefaced with ACC and runtime with RUN
  * Split all metrics into these two classes
- Automatically compare current to baseline
  * Detect how many current matched baseline files
  * Detect how many baseline went unmatched
  * Compute how many of the matched ACC_ files had changes
  * List the changed files
- Object Trackers: Very fast way to detect changes
  * Have a simple change detection regression so that the point results changed
    can be easily localized