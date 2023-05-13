package com.kinghouser;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class WorkingExample {

    public static void start() {
        // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Load the cascade classifier for detecting faces
        CascadeClassifier faceDetector = new CascadeClassifier("/opt/local/libexec/opencv4/share/data/haarcascades/haarcascade_frontalface_default.xml");

        // Load the cascade classifier for detecting eyes
        CascadeClassifier eyeDetector = new CascadeClassifier("/opt/local/libexec/opencv4/share/data/haarcascades/haarcascade_eye.xml");

        // Open the video capture device
        VideoCapture capture = new VideoCapture(0);

        // Loop over the frames in the video stream
        while (capture.isOpened()) {
            // Read a frame from the video stream
            Mat frame = new Mat();
            capture.read(frame);

            // Convert the frame to grayscale for processing
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            // Detect faces in the frame
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(gray, faces);

            // Loop over the detected faces and detect eyes in each face
            for (Rect face : faces.toArray()) {
                Mat faceROI = gray.submat(face);

                // Detect eyes in the face ROI
                MatOfRect eyes = new MatOfRect();
                eyeDetector.detectMultiScale(faceROI, eyes);

                // Loop over the detected eyes and draw a rectangle around each one
                for (Rect eye : eyes.toArray()) {
                    Point center = new Point(face.x + eye.x + (double) eye.width / 2, face.y + eye.y + (double) eye.height / 2);
                    Imgproc.ellipse(frame, center, new Size((double) eye.width / 2, (double) eye.height / 2), 0, 0, 360, new Scalar(255, 0, 255), 2);
                }
            }


            // Show the frame with the detected eyes
            Core.flip(frame, frame, 1);
            HighGui.imshow("Eye Tracker", frame);
            HighGui.waitKey(1);
        }
    }
}