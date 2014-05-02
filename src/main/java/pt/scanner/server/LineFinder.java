/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_PROBABILISTIC;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughLines2;
import java.util.stream.IntStream;
import pt.scanner.server.data.Contour;
import static pt.scanner.server.data.Contour.STORAGE;
import pt.scanner.server.data.Line;

public class LineFinder implements Runnable
{

	private final Contour contour;

	public LineFinder(Contour contour)
	{
		this.contour = contour;
	}

	@Override
	public void run()
	{
		CvSeq lines = cvHoughLines2(contour.getImage(), STORAGE, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 50, 50, 10);
		IntStream.range(0, lines.total()).forEach(i -> contour.addLine(new Line(new CvPoint(cvGetSeqElem(lines, i)).position(0),
				new CvPoint(cvGetSeqElem(lines, i)).position(1))));
	}
}
