package com.mechaniciancombat.client;

/** r396 angle rules; independent of the player entity and render interpolation. */
public final class ShoulderCameraAngles {
    private float yaw;
    private float pitch;

    public ShoulderCameraAngles(float yaw, float pitch) { reset(yaw, pitch); }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    public void reset(float yaw, float pitch) {
        this.yaw = wrap(yaw);
        this.pitch = Math.max(-90, Math.min(90, pitch));
    }

    public void turn(double deltaX, double deltaY) {
        reset(yaw + (float) deltaX * 0.15F, pitch + (float) deltaY * 0.15F);
    }

    public static float steerYaw(float bodyYaw, float cameraYaw, float sideways) {
        float target = wrap(cameraYaw - Math.max(-1, Math.min(1, sideways)) * 45.0F);
        float delta = wrap(target - bodyYaw);
        return wrap(bodyYaw + Math.max(-4, Math.min(4, delta)));
    }

    private static float wrap(float angle) {
        float wrapped = angle % 360.0F;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }
}
