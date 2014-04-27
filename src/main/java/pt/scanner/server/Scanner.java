/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.Pointer;
import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar.*;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Contour;
import pt.scanner.server.data.Line;
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
			IplImage initImg = cvLoadImage(String.format("%s/calibration/background.jpg", root), CV_LOAD_IMAGE_COLOR);
			IplImage grayImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvCvtColor(initImg, grayImg, CV_BGR2GRAY);
			IplImage binImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvInRangeS(grayImg, BLACK, CUT_GRAY, binImg);
			CvMemStorage storage = CvMemStorage.create();
			CvSeq contours = new CvSeq();
			// Finding Contours
			cvFindContours(binImg, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS);
			List<Contour> squares = new LinkedList<>();
			while (contours != null && !contours.isNull())
			{
				double area = cvContourArea(contours, CV_WHOLE_SEQ, 1);
				if (Double.compare(area, 20000.0) > 0)
				{
					squares.add(new Contour(contours, binImg));
				}
				contours = contours.h_next();
			}
			System.out.println(String.format("total: %s", squares.size()));
			// remove biggest contour
			squares.sort(null);
			squares.remove(squares.size() - 1);
			squares.parallelStream().forEach(s ->
			{
				// Finding Lines
				CvSeq lines = cvHoughLines2(s.getImage(), storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 50, 50, 10);
				System.out.println(String.format("total: %s", lines.total()));
				Random rand = new Random();
				Stack<Line> found = new Stack<>();
				for (int i = 0; i < lines.total(); i++)
				{
					Pointer line = cvGetSeqElem(lines, i);
					found.push(new Line(new CvPoint(line).position(0), new CvPoint(line).position(1)));
					found.lastElement().expand(s.getImage().width(), s.getImage().height());
					log.info(String.format("Line spotted: %s - %s - %s - %s", found.peek().start(), found.peek().end(), found.peek().angle(), found.peek().getLength()));
					// draw the segment on the image
					cvLine(initImg, found.peek().start(), found.peek().end(), CV_RGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)), 3, CV_AA, 0);
				}
				cvCircle(initImg, s.getCentroid(), 10, WHITE, CV_FILLED, 8, 0);
			});
			Utils.showImage(initImg, 640, 480);
			squares.parallelStream().forEach(Contour::release);
			cvReleaseImage(initImg);
			cvReleaseImage(binImg);
			storage.release();
		} catch (RuntimeException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
