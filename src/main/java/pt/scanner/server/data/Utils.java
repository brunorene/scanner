/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.googlecode.javacv.cpp.opencv_core;
import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import static com.googlecode.javacv.cpp.opencv_highgui.cvShowImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWaitKey;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

/**
 *
 * @author brsantos
 */
public class Utils
{

	public static <T extends Comparable<T>> Boolean between(T start, T value, T end)
	{
		return between(start, value, end, BetweenMode.IN_IN);
	}

	public static <T extends Comparable<T>> Boolean between(T start, T value, T end, BetweenMode mode)
	{
		switch (mode)
		{
			case IN_IN:
				return start.compareTo(value) <= 0 && value.compareTo(end) <= 0;
			case IN_EX:
				return start.compareTo(value) <= 0 && value.compareTo(end) < 0;
			case EX_IN:
				return start.compareTo(value) < 0 && value.compareTo(end) <= 0;
			case EX_EX:
				return start.compareTo(value) < 0 && value.compareTo(end) < 0;
		}
		return false;
	}

	public static void showImage(opencv_core.IplImage img, int width, int height)
	{
		opencv_core.IplImage showImg = cvCreateImage(new CvSize(width, height), IPL_DEPTH_8U, img.nChannels());
		cvResize(img, showImg);
		cvShowImage("Image", showImg);
		cvWaitKey(20000);
		cvReleaseImage(showImg);
	}

	public enum BetweenMode
	{

		IN_IN, IN_EX, EX_IN, EX_EX
	}
}