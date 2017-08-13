## Version 0.27
* Change in RANSAC implementation has caused several results to change.
** Affected algorithms: Visual Odometry
** This change does not change the algoritm just its implementation and with a larger sample set should produce the same results
** It's suspected that results on the whole got worse because algorithms were over tuned. Getting worse was dumb luck.


