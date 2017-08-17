## Version 0.27
* Change in RANSAC implementation has caused several results to change.
** Affected algorithms: Visual Odometry
** This change does not change the algoritm only the implementation and the numbers it draws.
** It's suspected that results on the whole got worse because algorithms were over tuned. Getting worse was dumb luck.
* Slight change in fiducial solutions
** Not sure. It's within floating point tolerance and is assumed to be a minor change in tuning


