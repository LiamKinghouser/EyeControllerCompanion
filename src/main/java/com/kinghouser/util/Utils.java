package com.kinghouser.util;

import com.kinghouser.EyeController;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {

    private static Robot robot;

    public static Point initialFacePos = null;

    public static Point screenCenter = null;
    public static Point screenTopLeft = null;
    public static Point screenTopRight = null;
    public static Point screenBottomLeft = null;
    public static Point screenBottomRight = null;

    public static boolean ready = false;
    public static boolean calibrationComplete = false;

    public static Scalar BLACK = new Scalar(0, 0, 0);
    public static Scalar GRAY = new Scalar(128, 128, 128);
    public static Scalar WHITE = new Scalar(255, 255, 255);
    public static Scalar RED = new Scalar(255, 0, 0);
    public static Scalar GREEN = new Scalar(0, 255, 0);
    public static Scalar BLUE = new Scalar(0, 0, 255);
    public static Scalar YELLOW = new Scalar(255, 255, 0);
    public static Scalar PURPLE = new Scalar(175, 0, 255);
    public static Scalar ORANGE = new Scalar(255, 175, 0);
    public static Scalar LIGHTRED = new Scalar(255, 0, 128);
    public static Scalar LIGHTGREEN = new Scalar(128, 255, 128);
    public static Scalar LIGHTBLUE = new Scalar(128, 255, 255);

    public static final String CONFIRM_PROMPT = "Look at the red square each time it moves and click \"Confirm\" when ready";

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(String message, Socket client) {
        try {
            OutputStream output = client.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadNativeLibrary() {
        Path jarPath;
        try {
            jarPath = Paths.get(EyeController.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String path = jarPath.getParent().toString() + File.separator + "lib" + File.separator + "libopencv_java460.dylib";
        System.load(path);
    }

    public static void calibrate() {
        JFrame calibrate = new JFrame("Calibrate");
        calibrate.setLocationRelativeTo(null);
        calibrate.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        calibrate.setLayout(null);
        calibrate.getContentPane().setLayout(null);
        calibrate.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (calibrationComplete) {
                    e.getWindow().dispose();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    EyeController.getEyeTrackerThread().startTracking();
                }
            }
        });

        JPanel point = new JPanel();
        point.setBackground(Color.RED);
        point.setSize(new Dimension(10, 10));
        point.setLocation((int) ((Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) - 10), (int) ((Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2) - 10));

        JLabel confirmPrompt = new JLabel(CONFIRM_PROMPT, SwingConstants.CENTER);
        confirmPrompt.setSize(600, 40);
        int y2 = (3 * (calibrate.getHeight() / 4)) -  40;
        confirmPrompt.setLocation((int) ((Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) - (confirmPrompt.getWidth() / 2)), y2);

        JButton confirm = new JButton("Confirm");
        confirm.setSize(120, 40);
        confirm.setLocation((int) ((Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2) - (confirm.getWidth() / 2)), (int) ((Toolkit.getDefaultToolkit().getScreenSize().getHeight()) - (20 + confirm.getHeight())));
        confirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        confirm.addActionListener(e -> {
            if (calibrationComplete) {
                calibrate.dispatchEvent(new WindowEvent(calibrate, WindowEvent.WINDOW_CLOSING));
            }
            else {
                confirm.setEnabled(false);
                confirmPrompt.setText("Calibrating...");
                ready = true;
            }
        });

        calibrate.add(point);
        calibrate.add(confirm);
        calibrate.add(confirmPrompt);
        confirm.setSelected(false);

        calibrate.setVisible(true);

        maximizeCalibrationWindow();

        EyeController.getEyeTrackerThread().beginCalibration(point, confirmPrompt, confirm);
    }

    private static void maximizeCalibrationWindow() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = runtimeName.split("@")[0];
        try {
            Thread.sleep(1);
            String applescriptCommand = "tell application \"System Events\"\n" +
                    "set value of attribute \"AXFullScreen\" of front window of (first process whose unix id is " + pid + ") to true\n" +
                    "end tell";

            String[] args = { "osascript", "-e", applescriptCommand };
            Process process = Runtime.getRuntime().exec(args);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateCursor(org.opencv.core.Point newPoint, org.opencv.core.Point face) {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // double displacement = new org.opencv.core.Point((double) newPoint.x) - screenCenter;
        // int normalizedDisplacement = displacement / maxDisplacement


    }

    private static Point calculateNewMouseLocation(Point newPoint) {
        return new Point(screenCenter.x + newPoint.x, screenCenter.y + newPoint.y);
    }

    public static void moveMouse(int x1, int y1, int x2, int y2, int n) {
        try {
            Robot r = new Robot();
            double dx = (x2 - x1) / ((double) n);
            double dy = (y2 - y1) / ((double) n);
            for (int step = 1; step <= n; step++) {
                r.mouseMove((int) (x1 + dx * step), (int) (y1 + dy * step));
            }
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void showFrame(Mat frame) {
        Core.flip(frame, frame, 1);
        HighGui.imshow("Eye Tracker", frame);
        HighGui.waitKey(1);
        System.out.println("Showed frame");
    }

    public static Rect[] getEyes(Rect[] eyes) {
        sortByArea(eyes);
        for (Rect eye : eyes) {
            ArrayList<Rect> matches = new ArrayList<>();
            matches.add(eye);
            int width = eye.width;
            int height = eye.height;
            // int x = eye.x;
            int y = eye.y;
            for (Rect eye2 : eyes) {
                if (eye == eye2) continue;
                if (Math.abs(width - eye2.width) <= 50 && Math.abs(height - eye2.height) <= 50 && Math.abs(y - eye2.y) <= 10 && matches.size() < 2) {
                    matches.add(eye2);
                }
            }
            if (matches.size() == 2) {
                return matches.toArray(new Rect[0]);
            }
        }
        return null;
    }

    public static void sortByArea(Rect[] eyes) {
        Arrays.sort(eyes, (rect1, rect2) -> Integer.compare((int) rect2.area(), (int) rect1.area()));
    }

    public static Rect getLargest(MatOfRect faces) {
        Rect largest = faces.toArray()[0];
        for (Rect face : faces.toArray()) {
            double area = face.area();
            if (area > largest.area()) largest = face;
        }
        return largest;
    }

    public static int getCenterX(Rect face) {
        return face.x + (face.width / 2);
    }

    public static int getCenterY(Rect face) {
        return face.y + (face.height / 2);
    }
}