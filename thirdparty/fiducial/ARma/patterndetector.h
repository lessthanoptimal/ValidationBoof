#ifndef _ARMA_DETECTOR_H_
#define _ARMA_DETECTOR_H_
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "pattern.h"

using namespace std;
using namespace cv;


namespace ARma
{
	
class PatternDetector
{
public:

    //constructor
	PatternDetector(const double param1, const double param2, const int param3, const  double param4, const int param5, const int thresh_mode);

	//distractor
	~PatternDetector(){};

enum THRES_MODE {
		FIXED_THRESHOLD,
		ADAPTIVE_THRESHOLD, 
	}; 

//detect patterns in the input frame
void detect(const Mat &frame, const Mat& cameraMatrix, const Mat& distortions, vector<Mat>& library, vector<Pattern>& foundPatterns);

private:

	int mode, normSize, block_size;
	double confThreshold, thresh1, thresh2;
	struct patInfo{
		int index;
		int ori;
		double maxCor;
	};
	Mat binImage, grayImage, normROI, patMask, patMaskInt;
	Point2f norm2DPts[4];


	void convertAndBinarize(const Mat& src, Mat& dst1, Mat& dst2, int thresh_mode = 1);
	void normalizePattern(const Mat& src, const Point2f roiPoints[], Rect& rec, Mat& dst);
	int identifyPattern(const Mat& src, std::vector<cv::Mat>& loadedPatterns, patInfo& out);

};


};
#endif
