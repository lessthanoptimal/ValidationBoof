/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of the SURF Performance Benchmark
 * (https://github.com/lessthanoptimal/SURFPerformance).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.metrics.surf;

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.describe.DescribeSurf_RadiusAngle;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FactorySurf {
	/**
	 * Java port of Pan-o-Matic's descriptor to make examing its behavior easier.
	 */
	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T,TupleDesc_F64> surfPanOMaticInBoofCV(boolean isOriented, Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
			orientation = FactoryOrientationAlgs.
					sliding_ii(new ConfigSlidingIntegral(0.65, Math.PI / 3.0, 8, -1, 6), integralType);

		DescribePointSurf<II> alg = new DescribePointSurfPanOMatic<II>(integralType);
		return new DescribeSurf_RadiusAngle<T,II>( alg , imageType );
	}

	/**
	 * Creates a BoofCV SURF descriptor
	 */
	public static <T extends ImageBase<T>, II extends ImageGray<II>>
	DescribePointRadiusAngle<T,TupleDesc_F64> surf( boolean stable , ImageType<T> imageType )
	{
		Class bandType = imageType.getDataType().getDataType();
		Class integralType = GIntegralImageOps.getIntegralType(bandType);

		DescribePointSurf<II> describe;
		OrientationIntegral<II> orientation;
		if( stable )
			orientation = FactoryOrientationAlgs.sliding_ii(null,integralType);
		else
			orientation = FactoryOrientationAlgs.average_ii(null,integralType);

		if( stable ) {
			describe = FactoryDescribeAlgs.surfStability(null,integralType);
		} else {
			describe = FactoryDescribeAlgs.surfSpeed(null,integralType);
		}

		if( ImageType.Family.GRAY == imageType.getFamily() )
			return new DescribeOrientationSurf(orientation,describe);
		else {
			DescribePointSurfPlanar descColor = new DescribePointSurfPlanar(describe,3);
			return new DescribeOrientationSurfColor(orientation,descColor,bandType,integralType);
		}
	}

	public static <T extends ImageGray<T>>
	DetectDescribePoint<T, TupleDesc_F64> detectDescribe(boolean stable , boolean color , Class<T> imageType  ) {

		ConfigFastHessian configDetect = new ConfigFastHessian(58,3, -1,1, 9, 4, 4);

		if( color ) {
			ImageType typeMS = ImageType.pl(3,imageType);
			if( stable )
				return FactoryDetectDescribe.surfColorStable(configDetect,null,null,typeMS);
			else
				return FactoryDetectDescribe.surfColorFast(configDetect,null,null,typeMS);
		} else {
			if( stable )
				return FactoryDetectDescribe.surfStable(configDetect,null,null,imageType);
			else
				return FactoryDetectDescribe.surfFast(configDetect,null,null,imageType);
		}
	}
}
