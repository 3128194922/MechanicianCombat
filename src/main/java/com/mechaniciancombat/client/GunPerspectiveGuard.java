package com.mechaniciancombat.client;

/** Keeps the gun's third-person view without overwriting the pre-gun perspective. */
final class GunPerspectiveGuard<T> {
    interface Camera<T> {
        T current();
        boolean enterShoulder();
        boolean isShoulder();
        boolean ensureShoulder();
        boolean isBack();
        void useBack();
        void leaveShoulder();
        void restore(T previous);
        void onRelease();
    }
    private boolean active;
    private boolean shoulder;
    private T previous;
    void update(boolean holding, Camera<T> camera) {
        if (holding) {
            if (!active) {
                previous = camera.current();
                shoulder = camera.enterShoulder();
                active = true;
                if (!shoulder) camera.useBack();
            } else if (shoulder) {
                if (!camera.isShoulder() && !camera.ensureShoulder()) camera.useBack();
            } else if (!camera.isBack()) {
                camera.useBack();
            }
        } else if (active) {
            active = false;
            camera.onRelease();
            camera.leaveShoulder();
            if (previous != null) camera.restore(previous);
            previous = null;
            shoulder = false;
        }
    }
    void reset() { active = false; shoulder = false; previous = null; }
}
