/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.google.common.base.Joiner;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Contour;
import static pt.scanner.server.data.Contour.STORAGE;
import pt.scanner.server.data.Line;
import pt.scanner.server.data.Position;
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
            squares.sort((s1, s2) -> s1.getLineCount() - s2.getLineCount());
            squares.remove(squares.size() - 1);
            squares.sort((s1, s2) -> s1.getIndex().intValue() - s2.getIndex().intValue());
            // relating mainLines
            squares.forEach(s -> s.joinLines());
            Position.quadrants().forEach(pos ->
            {
                squares.forEach(c1 ->
                {
                    Set<Line> l1 = c1.mainLines(pos);
                    if (!l1.isEmpty())
                    {
                        Line f1 = l1.iterator().next();
                        squares.forEach(c2 ->
                        {
                            Set<Line> l2 = c2.mainLines(pos);
                            if (!l2.isEmpty())
                            {
                                Line f2 = l2.iterator().next();
                                if (!c1.equals(c2) && !l1.isEmpty() && !l2.isEmpty()
                                        && ((f1.intersection(f2) != null && f1.angleDiff(f2) < 15)
                                        || (f1.intersection(f2) == null && f1.distancePerpendicular(f2.midPoint()) < 50)))
                                {
                                    f1.addRelated(c2);
                                    f2.addRelated(c1);
                                    l1.add(f2);
                                    l2.add(f1);
                                }
                            }
                        });
                    }
                });
            });
            Position.quadrants().forEach(pos ->
            {
                squares.stream().filter(c -> (c.mainLines(pos).size() > 0)).forEach(c ->
                {
                    c.mainLines(pos);
                    Line mean = Line.meanLine(c.mainLines(pos));
                    mean.getRelated().addAll(c.mainLines(pos).iterator().next().getRelated());
                    log.info("{} {} {}", pos, c, c.mainLines(pos));
                    c.mainLines(pos).clear();
                    c.mainLines(pos).add(mean);
                    cvLine(initImg, mean.start(), mean.end(), Position.colorQuadrants().get(pos), 3, CV_AA, 0);
                });
            });
            Utils.showImage(initImg, 640, 480, 20000);
            squares.forEach(s -> log.info("{}",
                    Position.quadrants().stream()
                    .collect(Collectors.toMap(
                                    q -> q,
                                    q -> s.mainLines(q).stream().map(l -> l.getRelated().size()).findFirst()))));
            squares.forEach(Contour::release);
            cvReleaseImage(initImg);
            cvReleaseImage(binImg);
            STORAGE.release();
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
        }
    }
}
