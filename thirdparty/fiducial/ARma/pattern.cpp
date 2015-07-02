#include "pattern.h"
#include <iostream>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;
using namespace std;

namespace ARma {


	int loadPattern(const char* filename, std::vector<cv::Mat>& library, bool add_border ){
		Mat img = imread(filename,0);

		if( img.data == NULL ) {
			cout << "Couldn't find the file " << filename << endl;
			return -1;
		}

		if(img.cols!=img.rows){
			cout << "Not a square pattern" << endl;
			return -1;
		}

		if( add_border ) {
			int r2 = img.rows/2;
			Mat M(img.rows*2,img.cols*2, img.type(), Scalar(0,0,0));
			img.copyTo(M(Rect(r2, r2, img.rows, img.rows) ));
			img.create(M.rows,M.cols,img.type());
			M.copyTo(img);

		}

		int msize = PAT_SIZE;

		Mat src(msize, msize, CV_8UC1);
		Point2f center((msize-1)/2.0f,(msize-1)/2.0f);
		Mat rot_mat(2,3,CV_32F);

		resize(img, src, Size(msize,msize));
		Mat subImg = src(Range(msize/4,3*msize/4), Range(msize/4,3*msize/4));
		library.push_back(subImg);

		rot_mat = getRotationMatrix2D( center, 90, 1.0);

		for (int i=1; i<4; i++){
			Mat dst= Mat(msize, msize, CV_8UC1);
			rot_mat = getRotationMatrix2D( center, -i*90, 1.0);
			warpAffine( src, dst , rot_mat, Size(msize,msize));
			Mat subImg = dst(Range(msize/4,3*msize/4), Range(msize/4,3*msize/4));
			library.push_back(subImg);
		}

		return 1;
	}

	Pattern::Pattern(double param1){
		id =-1;
		size = param1;
		orientation = -1;
		confidence = -1;

		rotVec = (Mat_<float>(3,1) << 0, 0, 0);
		transVec = (Mat_<float>(3,1) << 0, 0, 0);
		rotMat = Mat::eye(3, 3, CV_32F);
	}

	//convert rotation vector to rotation matrix (if you want to proceed with other libraries)
	void Pattern::rotationMatrix(const Mat& rotation_vector, Mat& rotation_matrix)
	{
		Rodrigues(rotation_vector, rotation_matrix);
	}

	Mat& Pattern::getRotationMatrix() {
		rotationMatrix(rotVec,rotMat);
		return rotMat;
	}

	void Pattern::showPattern()
	{
		cout << "Pattern ID: " << id << endl;
		cout << "Pattern Size: " << size << endl;
		cout << "Pattern Confedince Value: " << confidence << endl;
		cout << "Pattern Orientation: " << orientation << endl;
		rotationMatrix(rotVec, rotMat);
		cout << "Exterior Matrix (from pattern to camera): " << endl;
		for (int i = 0; i<3; i++){
		cout << rotMat.at<float>(i,0) << "\t" << rotMat.at<float>(i,1) << "\t" << rotMat.at<float>(i,2) << " |\t"<< transVec.at<float>(i,0) << endl;
		}
	}

	void Pattern::getExtrinsics(float patternWidth, const Mat& cameraMatrix, const Mat& distortions)
	{

		CvMat objectPts;//header for 3D points of pat3Dpts
		CvMat imagePts;//header for 2D image points of pat2Dpts 
		CvMat intrinsics = cameraMatrix;
		CvMat distCoeff = distortions;
		CvMat rot = rotVec;
		CvMat tra = transVec;
//		CvMat rotationMatrix = rotMat; // projectionMatrix = [rotMat tra];

		CvPoint2D32f pat2DPts[4];
		for (int i = 0; i<4; i++){
			pat2DPts[i].x = this->vertices.at(i).x;
			pat2DPts[i].y = this->vertices.at(i).y;
		}

		//3D points in pattern coordinate system
		CvPoint3D32f pat3DPts[4];
		pat3DPts[0].x = 0.0;
		pat3DPts[0].y = 0.0;
		pat3DPts[0].z = 0.0;
		pat3DPts[1].x = patternWidth;
		pat3DPts[1].y = 0.0;
		pat3DPts[1].z = 0.0;
		pat3DPts[2].x = patternWidth;
		pat3DPts[2].y = patternWidth;
		pat3DPts[2].z = 0.0;
		pat3DPts[3].x = 0.0;
		pat3DPts[3].y = patternWidth;
		pat3DPts[3].z = 0.0;

//		pat3DPts[0].x = -patternWidth/2;
//		pat3DPts[0].y = -patternWidth/2;
//		pat3DPts[0].z = 0.0;
//		pat3DPts[1].x = patternWidth/2;
//		pat3DPts[1].y = -patternWidth/2;
//		pat3DPts[1].z = 0.0;
//		pat3DPts[2].x = patternWidth/2;
//		pat3DPts[2].y = patternWidth/2;
//		pat3DPts[2].z = 0.0;
//		pat3DPts[3].x = -patternWidth/2.0;
//		pat3DPts[3].y = patternWidth/2;
//		pat3DPts[3].z = 0.0;

		cvInitMatHeader(&objectPts, 4, 3, CV_32FC1, pat3DPts);
		cvInitMatHeader(&imagePts, 4, 2, CV_32FC1, pat2DPts);
		
		//find extrinsic parameters
		cvFindExtrinsicCameraParams2(&objectPts, &imagePts, &intrinsics, &distCoeff, &rot, &tra);
	}

	void Pattern::draw(Mat& frame, const Mat& camMatrix, const Mat& distMatrix)
	{

//		cout << "detected " << id << endl;
		CvScalar color = cvScalar(255,255,255);
		
		switch (id){
			case 1:
				 color = cvScalar(255,0,255);
				break;
			case 2:
				 color = cvScalar(255,255,0);
				break;
			case 3:
				 color = cvScalar(0,255,255);
				break;

			default:
				color = cvScalar((50*id)%256,(75*id)%256,(120*id)%256);
				break;
		}

		//model 3D points: they must be projected to the image plane
		Mat modelPts = (Mat_<float>(8,3) << 0, 0, 0, size, 0, 0, size, size, 0, 0, size, 0,
			0, 0, -size, size, 0, -size, size, size, -size, 0, size, -size );


		std::vector<cv::Point2f> model2ImagePts;
		/* project model 3D points to the image. Points through the transformation matrix 
		(defined by rotVec and transVec) "are transfered" from the pattern CS to the 
		camera CS, and then, points are projected using camera parameters 
		(camera matrix, distortion matrix) from the camera 3D CS to its image plane
		*/
		projectPoints(modelPts, rotVec, transVec, camMatrix, distMatrix, model2ImagePts); 


		//draw cube, or whatever
		int i;
		for (i =0; i<4; i++){
			cv::line(frame, model2ImagePts.at(i%4), model2ImagePts.at((i+1)%4), color, 3);
		}
		for (i =4; i<7; i++){
			cv::line(frame, model2ImagePts.at(i%8), model2ImagePts.at((i+1)%8), color, 3);
		}
		cv::line(frame, model2ImagePts.at(7), model2ImagePts.at(4), color, 3);
		for (i =0; i<4; i++){
			cv::line(frame, model2ImagePts.at(i), model2ImagePts.at(i+4), color, 3);
		}
		
		//draw the line that reflects the orientation. It indicates the bottom side of the pattern
		cv::line(frame, model2ImagePts.at(2), model2ImagePts.at(3), cvScalar(80,255,80), 3);

		model2ImagePts.clear();

	}

}