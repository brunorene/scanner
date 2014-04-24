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
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Line;

/**
 *
 * @author brsantos
 */
public class Scanner implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Scanner.class);
	private static final CvScalar CUT_GRAY = new CvScalar(50, 0, 0, 0);

	private final String root;

	public Scanner(String root) {
		this.root = root;
	}

	@Override
	public void run() {
		try {
			IplImage initImg = cvLoadImage(String.format("%s/calibration/background.jpg", root), CV_LOAD_IMAGE_COLOR);
			IplImage grayImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvCvtColor(initImg, grayImg, CV_BGR2GRAY);
			IplImage binImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvInRangeS(grayImg, CvScalar.BLACK, CUT_GRAY, binImg);
			CvMemStorage storage = CvMemStorage.create();
			CvSeq contours = new CvSeq();
			// Finding Contours
			cvFindContours(binImg, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS);
			List<CvSeq> black = new LinkedList<>();
			List<CvSeq> squares = new LinkedList<>();
			while (contours != null && !contours.isNull()) {
				double area = cvContourArea(contours, CV_WHOLE_SEQ, 1);
				if (Double.compare(area, 20000.0) > 0) {
					squares.add(contours);
				} else {
					black.add(contours);
				}
				contours = contours.h_next();
			}
			System.out.println(String.format("total: %s", squares.size()));
			// remove small contours
			black.stream().forEach(b -> cvDrawContours(binImg, b, CvScalar.BLACK, CvScalar.BLACK, 0, CV_FILLED, 8));
			// remove biggest contour
			squares.sort((o1, o2) -> Double.compare(cvContourArea(o1, CV_WHOLE_SEQ, 1), cvContourArea(o2, CV_WHOLE_SEQ, 1)));
			CvSeq last = squares.remove(squares.size() - 1);
			cvDrawContours(binImg, last, CvScalar.BLACK, CvScalar.BLACK, 0, CV_FILLED, 8);
			Stack<IplImage> singles = new Stack<>();
			squares.stream().forEach(s -> {
				singles.push(cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1));
				cvSet(singles.peek(), CvScalar.BLACK);
				cvDrawContours(singles.peek(), s, CvScalar.BLACK, CvScalar.BLACK, 0, CV_FILLED, 8);
				cvDrawContours(singles.peek(), s, CvScalar.WHITE, CvScalar.WHITE, 0, 2, 8);
				//showImage(singles.peek(), new CvSize(640, 480));
				// Finding Lines
				CvSeq lines = cvHoughLines2(singles.peek(), storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 50, 50, 10);
				System.out.println(String.format("total: %s", lines.total()));
				Random rand = new Random();
				Stack<Line> found = new Stack<>();
				for (int i = 0; i < lines.total(); i++) {
					Pointer line = cvGetSeqElem(lines, i);
					found.push(new Line(new CvPoint(line).position(0), new CvPoint(line).position(1)));
					System.out.println(String.format("Line spotted: %s - %s - %s - %s", found.peek().start(), found.peek().end(), found.peek().angle(), found.peek().getLength()));
					// draw the segment on the image
					cvLine(initImg, found.peek().start(), found.peek().end(), CV_RGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)), 3, CV_AA, 0);
				}
			});
			showImage(initImg, new CvSize(640, 480));
			cvReleaseImage(initImg);
			cvReleaseImage(binImg);
			storage.release();
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void showImage(IplImage img, CvSize size) {
		IplImage showImg = cvCreateImage(size, IPL_DEPTH_8U, img.nChannels());
		cvResize(img, showImg);
		cvShowImage("Image", showImg);
		cvWaitKey(20000);
		cvReleaseImage(showImg);
	}

	public static void main(String[] args) {
		Scanner scanner = new Scanner("/home/brsantos/MEOCloud/Scanner3D");
		scanner.run();
	}
}
