package com.mechaniciancombat.client;

/** Plain-JDK regression checks for the independent r396 shoulder-camera angles. */
public final class ShoulderCameraAnglesContract {
    public static void main(String[] args) {
        ShoulderCameraAngles camera = new ShoulderCameraAngles(179.0F, 0.0F);
        camera.turn(20.0D, 1000.0D);
        close(camera.yaw(), -178.0F, "mouse yaw must wrap without taking the body yaw");
        close(camera.pitch(), 90.0F, "pitch must clamp at +90");
        camera.turn(0, -2000);
        close(camera.pitch(), -90.0F, "pitch must clamp at -90");
        close(ShoulderCameraAngles.steerYaw(0, 90, 1), 90, "running follows the camera immediately");
        close(ShoulderCameraAngles.steerYaw(0, -90, -1), -90, "strafe does not offset the camera heading");
        close(ShoulderCameraAngles.steerYaw(179, -179, 0), -179, "steering takes shortest wraparound path");
        camera.reset(40, 15);
        close(camera.yaw(), 40, "new session resets yaw");
        close(camera.pitch(), 15, "new session resets pitch");
        System.out.println("ShoulderCameraAnglesContract: 8 checks passed");
    }

    private static void close(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > 0.0001F) throw new AssertionError(message + ": " + actual);
    }
}
