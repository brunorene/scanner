/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.util;

import static org.bytedeco.javacpp.opencv_core.CV_FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_core.putText;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import com.vividsolutions.jts.geom.*;
import org.apache.commons.math3.util.*;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.*;

/**
 *
 * @author brsantos
 */
public class Utils
{

	public final static Scalar BLACK = new Scalar(0, 0, 0, 0);
	public final static Scalar GRAY = new Scalar(127, 127, 127, 0);
	public final static Scalar BLUE = new Scalar(255, 0, 0, 0);
	public final static Scalar CYAN = new Scalar(255, 255, 0, 0);
	public static final Scalar GREEN = new Scalar(0, 255, 0, 0);
	public final static Scalar MAGENTA = new Scalar(255, 0, 255, 0);
	public static final Scalar RED = new Scalar(0, 0, 255, 0);
	public static final Scalar WHITE = new Scalar(255, 255, 255, 255);
	public static final Scalar YELLOW = new Scalar(0, 255, 255, 0);
	private final static Logger log = LoggerFactory.getLogger(Utils.class);

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

	public static Point point(Coordinate c)
	{
		return new Point((int) FastMath.round(c.x), (int) FastMath.round(c.y));
	}

	public static void showImage(Mat img, float ratio, int millsTimeout)
	{
		Size showSize = new Size(FastMath.round(img.arrayWidth() * ratio), FastMath.round(img.arrayHeight() * ratio));
		Mat showImg = new Mat(showSize, img.type(), BLACK);
		resize(img, showImg, showSize);
		imshow("Image", showImg);
		waitKey(millsTimeout);
	}

	public static void writeText(Mat img, double size, int x, int y, Scalar color, String text)
	{
		putText(img, text, new Point(x, y), CV_FONT_HERSHEY_SIMPLEX, 1.0, color);
	}

	public enum BetweenMode
	{

		IN_IN, IN_EX, EX_IN, EX_EX
	}
}
