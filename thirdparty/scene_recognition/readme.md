# Data Sets

## ukbench

Used in [1] to evaluate their results. No longer hosted at [2] but can now be downloaded from [here](https://archive.org/details/ukbench).
This is composed of 10,000 images. Every object has 4 photos from different perspectives.

- [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree." Computer vision and pattern r
ecognition, 2006 IEEE computer society conference on. Vol. 2. Ieee, 2006.
- [2] http://www.vis.uky.edu/∼stewe/ukbench

## INRIA Holidays

Various holiday photos that have been grounded into variable number of related images.
Dataset size: 1491 images in total: 500 queries and 991 corresponding relevant images.

- https://lear.inrialpes.fr/~jegou/data.php
- "Hamming Embedding and Weak geometry consistency for large scale image search"
  Proceedings of the 10th European conference on Computer vision, October, 2008
  
## Flicker

As in referenced papers, these images are used as distractors and for stress testing. Both MIRFLICKR-25000 and 
MIRFLICKR-1M are used. The 1M dataset is quite massive, so a download script to automate the process has been provided.

- https://press.liacs.nl/mirflickr/


# Plotting

```bash
python3 -m venv venv
source venv/bin/activate
pip3 install -r requirements.txt
python3 plot_tuning.py
```
