package com.kinghouser;

import com.kinghouser.util.EyeTrackerThread;
import com.kinghouser.util.Server;
import com.kinghouser.util.Utils;

public class EyeController {

    private static Server server;
    private static EyeTrackerThread eyeTrackerThread;

    public static void main(String[] args) {
        Utils.loadNativeLibrary();
        server = new Server();
        server.start();
        eyeTrackerThread = new EyeTrackerThread();
        eyeTrackerThread.start();
    }

    public static Server getServer() {
        return server;
    }

    public static EyeTrackerThread getEyeTrackerThread() {
        return eyeTrackerThread;
    }
}