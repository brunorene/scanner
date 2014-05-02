/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.googlecode.javacpp.Loader;
import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar.*;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Contour;
import static pt.scanner.server.data.Contour.STORAGE;
import pt.scanner.server.data.Utils;

/**
 *
 * @author brsantos
 */
public class Scanner implements Runnable
{

	private static final CvScalar CUT_GRAY = new CvScalar(50, 0, 0, 0);
	private static final Logger log = LoggerFactory.getLogger(Scanner.class);

	public static void main(String[] args)
	{
		Scanner scanner = new Scanner("/home/brsantos/MEOCloud/Scanner3D");
		scanner.run();
	}
	private final String root;

	public Scanner(String root)
	{
		this.root = root;
	}

	@Override
	public void run()
	{
		try
		{
			ExecutorService service = Executors.newFixedThreadPool(4);
			IplImage initImg = cvLoadImage(String.format("%s/calibration/background.jpg", root), CV_LOAD_IMAGE_COLOR);
			IplImage grayImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvCvtColor(initImg, grayImg, CV_BGR2GRAY);
			IplImage binImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvInRangeS(grayImg, BLACK, CUT_GRAY, binImg);
			CvSeq contours = new CvSeq();
			// Finding Contours
			cvFindContours(binImg, STORAGE, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS);
			List<Contour> squares = new LinkedList<>();
			while (contours != null && !contours.isNull())
			{
				if (Double.compare(cvContourArea(contours, CV_WHOLE_SEQ, 1), 20000.0) > 0)
				{
					squares.add(new Contour(contours, binImg));
				}
				contours = contours.h_next();
			}
			// remove biggest contour
			squares.sort(null);
			squares.remove(squares.size() - 1);
			squares.forEach(s -> service.submit(new LineFinder(s)));
			Utils.showImage(initImg, 640, 480);
			squares.forEach(Contour::release);
			cvReleaseImage(initImg);
			cvReleaseImage(binImg);
			STORAGE.release();
		} catch (RuntimeException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
