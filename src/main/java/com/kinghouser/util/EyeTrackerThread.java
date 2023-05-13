package com.kinghouser.util;

import com.kinghouser.EyeController;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EyeTrackerThread extends Thread {

    private final int[] starBitMask = { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1,
            1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0,
            0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 };

    private final int[] ringBitMask = { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1,
            1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0,
            1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 };

    private final long start = 0;
    private final long end = 0;
    private final long time = 0;

    private RotatedRect pupil = new RotatedRect();
    private Point axisA = null;
    private Point axisB = null;
    private int pupilMajorAxis = 0;
    private int pupilMinorAxis = 0;
    private final boolean star = false;
    private final boolean ring = false;

    private final boolean debug = false;

    private VideoCapture capture;

    public void run() {
        // Utils.calibrate();
        startTracking();
    }

    public void beginCalibration(JPanel dot, JLabel confirmPrompt, JButton confirm) {
        Path jarPath;
        try {
            jarPath = Paths.get(EyeController.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String jarDirPath = jarPath.getParent().toString();
        String xmlPath = jarDirPath + File.separator + "lib" + File.separator + "xml";
        CascadeClassifier faceDetector = new CascadeClassifier(xmlPath + File.separator + "haarcascades/haarcascade_frontalface_default.xml");

        CascadeClassifier eyeDetector = new CascadeClassifier(xmlPath + File.separator + "haarcascades/haarcascade_eye.xml");

        capture = new VideoCapture(0);

        while (capture.isOpened()) {
            if (!Utils.ready) {
                continue;
            }
            Mat frame = new Mat();
            capture.read(frame);

            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            MatOfRect faces = new MatOfRect();
            if (faceDetector.empty()) continue;
            faceDetector.detectMultiScale(gray, faces);

            if (faces.toArray().length == 0) continue;
            Rect face = Utils.getLargest(faces);
            Mat faceROI = new Mat(gray, face);

            Imgproc.rectangle(frame, face, new Scalar(255, 0, 255));

            MatOfRect eyes = new MatOfRect();
            eyeDetector.detectMultiScale(faceROI, eyes);
            ArrayList<org.opencv.core.Point> eyeCenters = new ArrayList<>();

            Rect[] definiteEyes = Utils.getEyes(eyes.toArray());
            if (definiteEyes == null) continue;

            for (Rect eye : definiteEyes) {
                org.opencv.core.Point center = new org.opencv.core.Point(face.x + eye.x + (double) eye.width / 2, face.y + eye.y + (double) eye.height / 2);
                eyeCenters.add(center);
            }

            double averageX = 0;
            double averageY = 0;
            for (org.opencv.core.Point point : eyeCenters) {
                averageX = averageX + point.x;
                averageY = averageY + point.y;
            }
            averageX = averageX / eyeCenters.size();
            averageY = averageY / eyeCenters.size();

            org.opencv.core.Point average = new org.opencv.core.Point(averageX, averageY);

            if (Utils.screenCenter == null) {
                Utils.screenCenter = new java.awt.Point((int) average.x, (int) average.y);
                Utils.initialFacePos = new java.awt.Point(face.x, face.y);
                dot.setLocation(0, 0);
            }
            else if (Utils.screenTopLeft == null) {
                Utils.screenTopLeft = new java.awt.Point((int) average.x, (int) average.y);
                dot.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - dot.getWidth(), 0);
            }
            else if (Utils.screenTopRight == null) {
                Utils.screenTopRight = new java.awt.Point((int) average.x, (int) average.y);
                dot.setLocation(0, Toolkit.getDefaultToolkit().getScreenSize().height - dot.getHeight());
            }
            else if (Utils.screenBottomLeft == null) {
                Utils.screenBottomLeft = new java.awt.Point((int) average.x, (int) average.y);
                dot.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - dot.getWidth(), Toolkit.getDefaultToolkit().getScreenSize().height - dot.getHeight());
            }
            else if (Utils.screenBottomRight == null) {
                Utils.screenBottomRight = new java.awt.Point((int) average.x, (int) average.y);
                dot.setVisible(false);
                Utils.calibrationComplete = true;
                confirm.setEnabled(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                confirm.setText("Close Window");
                confirmPrompt.setText("Calibration complete. Close this window to start Eye Tracker.");
                break;
            }

            Utils.ready = false;
            confirmPrompt.setText(Utils.CONFIRM_PROMPT);
            confirm.setEnabled(true);
        }
    }

    public void startTracking() {
        Path jarPath;
        try {
            jarPath = Paths.get(EyeController.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String jarDirPath = jarPath.getParent().toString();
        String xmlPath = jarDirPath + File.separator + "lib" + File.separator + "xml";
        CascadeClassifier faceDetector = new CascadeClassifier(xmlPath + File.separator + "haarcascades/haarcascade_frontalface_default.xml");

        CascadeClassifier eyeDetector = new CascadeClassifier(xmlPath + File.separator + "haarcascades/haarcascade_eye.xml");

        if (capture == null) capture = new VideoCapture(0);

        while (capture.isOpened()) {
            // if (EyeTracker.getServer().getClient() == null) continue;

            Mat frame = new Mat();
            capture.read(frame);
            System.out.println(frame.width());

            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            MatOfRect faces = new MatOfRect();
            if (faceDetector.empty()) {
                Utils.showFrame(frame);
                continue;
            }
            faceDetector.detectMultiScale(gray, faces);

            if (faces.toArray().length == 0) {
                Utils.showFrame(frame);
                continue;
            }
            Rect face = Utils.getLargest(faces);
            // System.out.println("Face center: " + (face.x + face.width / 2) + ", " + (face.y + face.height / 2));
            Mat faceROI = new Mat(gray, face);

            Imgproc.rectangle(frame, face, new Scalar(255, 0, 255));

            MatOfRect eyes = new MatOfRect();
            eyeDetector.detectMultiScale(faceROI, eyes);
            ArrayList<Point> eyeCenters = new ArrayList<>();

            for (Rect eye : eyes.toArray()) {
                System.out.println(eye.width + " " + eye.height);
            }

            Rect[] definiteEyes = Utils.getEyes(eyes.toArray());
            if (definiteEyes == null) {
                Utils.showFrame(frame);
                continue;
            }

            for (Rect eye : definiteEyes) {
                Point center = new Point(face.x + eye.x + (double) eye.width / 2, face.y + eye.y + (double) eye.height / 2);
                eyeCenters.add(center);
                //Imgproc.ellipse(frame, center, new Size((double) eye.width / 8, (double) eye.height / 8), 0, 0, 360, GREEN, 2);
                Imgproc.ellipse(frame, center, new Size(1, 1), 0, 0, 360, Utils.GREEN, 2);
            }

            double averageX = 0;
            double averageY = 0;
            for (Point point : eyeCenters) {
                averageX = averageX + point.x;
                averageY = averageY + point.y;
            }
            averageX = averageX / eyeCenters.size();
            averageY = averageY / eyeCenters.size();

            // System.out.println("Average point: " + averageX + ", " + averageY);
            Point average = new Point(averageX, averageY);
            Imgproc.ellipse(frame, average, new Size(1, 1), 0, 0, 360, new Scalar(255, 0, 255), 2);

            int coordX = gray.width() - Utils.getCenterX(face);
            int coordY = Utils.getCenterY(face);
            int height = gray.height();
            int width = gray.width();

            // String message = coordY + "," + coordX + "," + height + "," + width;
            // Utils.sendMessage(message, EyeTracker.getServer().getClient());

            // detectPupils();

            Utils.updateCursor(average, new Point(face.x, face.y));
            Utils.showFrame(frame);
        }
    }

    /*

    private void detectPupils() {
        int width = 320;
        int height = 240;

        // min and max pupil radius
        int r_min = 40;
        int r_max = 120;

        // min and max pupil diameter
        int d_min = 2 * r_min;
        int d_max = 2 * r_max;

        // min and max pupil area
        double area; //ww  w.  j  a va  2s  . c o m
        double a_min = Math.PI * r_min * r_min;
        double a_max = Math.PI * r_max * r_max;

        // histogram stuff
        List<Mat> images;
        MatOfInt channels;
        Mat mask;
        Mat hist;
        MatOfInt mHistSize;
        MatOfFloat mRanges;

        // contour and circle stuff
        Rect rect = null;
        Rect rectMin;
        Rect rectMax;
        List<MatOfPoint> contours;
        MatOfPoint3 circles;

        // pupil center
        Point p;

        // ellipse test points
        Point v;
        Point r;
        Point s;

        // rect points
        Point tl;
        Point br;

        // pupil edge detection
        Vector<Point> pointsTest;
        Vector<Point> pointsEllipse;
        Vector<Point> pointsRemoved;

        // temporary variables
        double distance;
        double rad;
        double length;
        int x;
        int y;
        int tmp;
        byte[] buff;

        // -------------------------------------------------------------------------------------------------------------
        // step 1
        // blur the image to reduce noise

        Mat tmp1 = new Mat(frame.rows(), frame.cols(), frame.type());
        Mat tmp2 = new Mat(frame.rows(), frame.cols(), frame.type());

        Imgproc.medianBlur(frame, tmp1, 25);

        // -------------------------------------------------------------------------------------------------------------
        // step 2
        // locate the pupil with feature detection and compute a histogram for each,
        // the best feature will be used as rough pupil location (rectMin)

        int score = 0;
        int winner = 0;

        // feature detection

        MatOfKeyPoint matOfKeyPoints = new MatOfKeyPoint();
        MSER detector = MSER.create();  // Maximal Stable Extremal Regions
        detector.detect(tmp1, matOfKeyPoints);

        List<KeyPoint> keyPoints = matOfKeyPoints.toList();

        // histogram calculation
        for (int i = 0; i < keyPoints.size(); i++) {
            x = (int) keyPoints.get(i).pt.x;
            y = (int) keyPoints.get(i).pt.y;
            tl = new Point(Math.max(x - 5, 0), Math.max(y - 5, 0));
            br = new Point(x + 5 < width ? x + 5 : width - 1, y + 5 < height ? y + 5 : height - 1);

            images = new ArrayList<>();
            images.add(tmp1.submat(new Rect(tl, br)));
            channels = new MatOfInt(0);
            mask = new Mat();
            hist = new Mat();
            mHistSize = new MatOfInt(256);
            mRanges = new MatOfFloat(0f, 256f);
            Imgproc.calcHist(images, channels, mask, hist, mHistSize, mRanges);

            tmp = 0;
            for (int j = 0; j < 256 / 3; j++) {
                tmp += (256 / 3 - j) * (int) hist.get(j, 0)[0];
            }
            if (tmp >= score) {
                score = tmp;
                winner = i;
                rect = new Rect(tl, br);
            }

            if (debug) {
                // show features (orange)
                Imgproc.circle(frame, new Point(x, y), 3, new Scalar(255, 0, 255));
            }
        }
        if (rect == null) {
            return;
        }
        rectMin = rect.clone();

        if (debug) {
            // show rectMin (red)
            Imgproc.rectangle(frame, rectMin.tl(), rect.br(), new Scalar(255, 0, 255), 1);
        }

        // -------------------------------------------------------------------------------------------------------------
        // step 3
        // compute a rectMax (blue) which is larger than the pupil

        int margin = 32;

        rect.x = rect.x - margin;
        rect.y = rect.y - margin;
        rect.width = rect.width + 2 * margin;
        rect.height = rect.height + 2 * margin;

        rectMax = rect.clone();

        if (debug) {
            // show features (orange)
            Imgproc.rectangle(frame, rectMax.tl(), rectMax.br(), new Scalar(255, 0, 255));
        }

        // -------------------------------------------------------------------------------------------------------------
        // step 4
        // blur the image again

        Imgproc.medianBlur(frame, tmp1, 7);
        Imgproc.medianBlur(tmp1, tmp1, 3);
        Imgproc.medianBlur(tmp1, tmp1, 3);
        Imgproc.medianBlur(tmp1, tmp1, 3);

        // -------------------------------------------------------------------------------------------------------------
        // step 5
        // detect edges

        Imgproc.Canny(tmp1, tmp2, 40, 50);

        // -------------------------------------------------------------------------------------------------------------
        // step 6
        // from pupil center to maxRect borders, find all edge points, compute a first ellipse

        p = new Point(rectMin.x + (double) rectMin.width / 2, rectMin.y + (double) rectMin.height / 2);
        pointsTest = new Vector<>();
        pointsEllipse = new Vector<>();
        pointsRemoved = new Vector<>();
        buff = new byte[tmp2.rows() * tmp2.cols()];
        tmp2.get(0, 0, buff);

        length = Math.min(p.x - rectMax.x - 3, p.y - rectMax.y - 3);
        length = Math.sqrt(2 * Math.pow(length, 2));
        Point z = new Point(p.x, p.y - length);
        for (int i = 0; i < 360; i += 15) {
            rad = Math.toRadians(i);
            x = (int) (p.x + Math.cos(rad) * (z.x - p.x) - Math.sin(rad) * (z.y - p.y));
            y = (int) (p.y + Math.sin(rad) * (z.x - p.x) - Math.cos(rad) * (z.y - p.y));
            pointsTest.add(new Point(x, y));
        }

        if (debug) {
            for (Point point : pointsTest) {
                Imgproc.line(frame, p, point, GRAY, 1);
                Imgproc.rectangle(frame, rectMin.tl(), rectMin.br(), GREEN, 1);
                Imgproc.rectangle(frame, rectMax.tl(), rectMax.br(), BLUE, 1);
            }
            // Imgproc.rectangle(frame, rectMin.tl(), rectMin.br(), BLACK, -1);
            Imgproc.rectangle(frame, rectMin.tl(), rectMin.br(), RED, 1);
            Imgproc.rectangle(frame, rectMax.tl(), rectMax.br(), BLUE);
        }

        for (Point point : pointsTest) {
            v = new Point(point.x, point.y);
            r = new Point(v.x - p.x, v.y - p.y);
            length = Math.sqrt(Math.pow(p.x - v.x, 2) + Math.pow(p.y - v.y, 2));
            boolean found = false;
            for (int j = 0; j < Math.round(length); j++) {
                s = new Point(Math.rint(p.x + (double) j / length * r.x),
                        Math.rint(p.y + (double) j / length * r.y));
                s.x = Math.max(1, Math.min(s.x, width - 2));
                s.y = Math.max(1, Math.min(s.y, height - 2));
                tl = new Point(s.x - 1, s.y - 1);
                br = new Point(s.x + 1, s.y + 1);
                buff = new byte[3 * 3];
                rect = new Rect(tl, br);
                try {
                    (tmp2.submat(rect)).get(0, 0, buff);
                    for (int k = 0; k < 3 * 3; k++) {
                        if (Math.abs(buff[k]) == 1) {
                            pointsEllipse.add(s);
                            found = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    break;
                }
                if (found) {
                    break;
                }
            }
        }

        double e_min = Double.POSITIVE_INFINITY;
        double e_max = 0;
        double e_med = 0;
        for (int i = 0; i < pointsEllipse.size(); i++) {
            v = pointsEllipse.get(i);
            length = Math.sqrt(Math.pow(p.x - v.x, 2) + Math.pow(p.y - v.y, 2));
            e_min = Math.min(length, e_min);
            e_max = Math.max(length, e_max);
            e_med = e_med + length;
        }
        e_med = e_med / pointsEllipse.size();
        if (pointsEllipse.size() >= 5) {
            Point[] points1 = new Point[pointsEllipse.size()];
            for (int i = 0; i < pointsEllipse.size(); i++) {
                points1[i] = pointsEllipse.get(i);
            }
            MatOfPoint2f points2 = new MatOfPoint2f();
            points2.fromArray(points1);
            pupil = Imgproc.fitEllipse(points2);
        }
        if (pupil.center.x == 0 && pupil.center.y == 0) {
            // something went wrong, return null
            reset();
            return;
        }

        // if (debug) {
            Imgproc.ellipse(frame, pupil, PURPLE, 2);
        // }

        // -------------------------------------------------------------------------------------------------------------
        // step 7
        // remove some outlier points and compute the ellipse again

        try {
            for (int i = 1; i <= 4; i++) {
                distance = 0;
                int remove = 0;
                for (int j = pointsEllipse.size() - 1; j >= 0; j--) {
                    v = pointsEllipse.get(j);
                    length = Math.sqrt(Math.pow(v.x - pupil.center.x, 2) + Math.pow(v.y - pupil.center.y, 2));
                    if (length > distance) {
                        distance = length;
                        remove = j;
                    }
                }
                v = pointsEllipse.get(remove);
                pointsEllipse.removeElementAt(remove);
                pointsRemoved.add(v);
            }
        } catch (Exception e) {
            // something went wrong, return null
            reset();
            return;
        }
        if (pointsEllipse.size() >= 5) {
            Point[] points1 = new Point[pointsEllipse.size()];
            for (int i = 0; i < pointsEllipse.size(); i++) {
                points1[i] = pointsEllipse.get(i);
            }
            MatOfPoint2f points2 = new MatOfPoint2f();
            points2.fromArray(points1);
            pupil = Imgproc.fitEllipse(points2);

            Point[] vertices = new Point[4];
            pupil.points(vertices);
            double d1 = Math
                    .sqrt(Math.pow(vertices[1].x - vertices[0].x, 2) + Math.pow(vertices[1].y - vertices[0].y, 2));
            double d2 = Math
                    .sqrt(Math.pow(vertices[2].x - vertices[1].x, 2) + Math.pow(vertices[2].y - vertices[1].y, 2));

            if (d1 >= d2) {
                pupilMajorAxis = (int) (d1 / 2);
                pupilMinorAxis = (int) (d2 / 2);
                axisA = new Point(vertices[1].x + (vertices[2].x - vertices[1].x) / 2,
                        vertices[1].y + (vertices[2].y - vertices[1].y) / 2);
                axisB = new Point(vertices[0].x + (vertices[1].x - vertices[0].x) / 2,
                        vertices[0].y + (vertices[1].y - vertices[0].y) / 2);
            } else {
                pupilMajorAxis = (int) (d2 / 2);
                pupilMinorAxis = (int) (d1 / 2);
                axisB = new Point(vertices[1].x + (vertices[2].x - vertices[1].x) / 2,
                        vertices[1].y + (vertices[2].y - vertices[1].y) / 2);
                axisA = new Point(vertices[0].x + (vertices[1].x - vertices[0].x) / 2,
                        vertices[0].y + (vertices[1].y - vertices[0].y) / 2);
            }
        }

        double ratio = (double) pupilMinorAxis / (double) pupilMajorAxis;
        if (ratio < 0.75 || 2 * pupilMinorAxis <= d_min || 2 * pupilMajorAxis >= d_max) {
            // something went wrong, return null
            reset();
            return;
        }

        // pupil found
        if (debug) {
            Imgproc.ellipse(frame, pupil, GREEN, 2);
            Imgproc.line(frame, pupil.center, axisA, RED, 2);
            Imgproc.line(frame, pupil.center, axisB, BLUE, 2);
            Imgproc.circle(frame, pupil.center, 1, GREEN, 0);

            x = 5;
            y = 5;
            // Imgproc.rectangle(frame, new Point(x, y), new Point(x + 80 + 4, y + 10), BLACK, -1);
            Imgproc.rectangle(frame, new Point(x + 2, y + 2), new Point(x + 2 + pupilMajorAxis, y + 4), RED, -1);
            Imgproc.rectangle(frame, new Point(x + 2, y + 6), new Point(x + 2 + pupilMinorAxis, y + 8), BLUE, -1);

            for (int i = pointsEllipse.size() - 1; i >= 0; i--) {
                Imgproc.circle(frame, pointsEllipse.get(i), 2, ORANGE, -1);
            }
            for (int i = pointsRemoved.size() - 1; i >= 0; i--) {
                Imgproc.circle(frame, pointsRemoved.get(i), 2, PURPLE, -1);
            }
        }
        Imgproc.ellipse(frame, pupil, GREEN, 2);
        Imgproc.circle(frame, pupil.center, 1, GREEN, 0);
    }

    */

    private void reset() {
        // something went wrong, return null
        pupil = new RotatedRect();
        axisA = null;
        axisB = null;
        pupilMajorAxis = 0;
        pupilMinorAxis = 0;
    }
}
