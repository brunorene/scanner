/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;
import com.vividsolutions.jts.geom.Coordinate;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contour implements Comparable<Contour>
{

	private static final Logger log = LoggerFactory.getLogger(Contour.class);
	private final CvSeq contour;
	private final IplImage image;
	private final Map<Position, Set<Line>> lines = new HashMap<>();
	private final CvMoments moments = new CvMoments();

	public Contour(CvSeq contour, IplImage img)
	{
		this.contour = contour;
		this.image = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
		cvSet(this.image, CvScalar.BLACK);
		cvDrawContours(this.image, contour, CvScalar.WHITE, CvScalar.WHITE, 0, 2, 8);
		cvMoments(image, moments, 1);
		float[] fp = new float[4];
		cvFitLine(contour, CV_DIST_L12, 0, 0.01, 0.01, fp);
		lines.put(Position.TOP, new HashSet<>());
		lines.put(Position.BOTTOM, new HashSet<>());
		lines.put(Position.RIGHT, new HashSet<>());
		lines.put(Position.LEFT, new HashSet<>());
		log.info("{}", Arrays.asList(ArrayUtils.toObject(fp)));
	}

	public void addLine(Line l)
	{
	}

	@Override
	public int compareTo(Contour o)
	{
		return Double.compare(cvContourArea(contour, CV_WHOLE_SEQ, 1), cvContourArea(o.getContour(), CV_WHOLE_SEQ, 1));
	}

	public CvPoint getCentroid()
	{
		CvPoint p = new CvPoint();
		p.x((int) Math.round(moments.m10() / moments.m00()));
		p.y((int) Math.round(moments.m01() / moments.m00()));
		return p;
	}

	public Coordinate getCentroidCoordinate()
	{
		return new Coordinate(moments.m10() / moments.m00(), moments.m01() / moments.m00());
	}

	public CvSeq getContour()
	{
		return contour;
	}

	public IplImage getImage()
	{
		return image;
	}

	public void release()
	{
		cvReleaseImage(image);
	}
}
