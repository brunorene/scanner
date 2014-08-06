/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import static org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_core.CvFont;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import static org.bytedeco.javacpp.opencv_highgui.cvShowImage;
import static org.bytedeco.javacpp.opencv_highgui.cvWaitKey;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;

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

    public static void showImage(CvMat img, int width, int height, int millsTimeout)
    {
        CvMat showImg = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, img.nChannels()).asCvMat();
        cvResize(img, showImg);
        cvShowImage("Image", showImg);
        cvWaitKey(millsTimeout);
		showImg.release();
    }

    public static void writeText(CvMat img, double size, int x, int y, CvScalar color, String text)
    {
        CvFont font = new CvFont();
        int fontFace = CV_FONT_HERSHEY_SIMPLEX;
        int thickness = 3;
        cvInitFont(font, fontFace, size, size, 0, thickness, CV_AA);
        cvPutText(img, text, cvPoint(x, y), font, color);
    }

    public enum BetweenMode
    {

        IN_IN, IN_EX, EX_IN, EX_EX
    }
}
