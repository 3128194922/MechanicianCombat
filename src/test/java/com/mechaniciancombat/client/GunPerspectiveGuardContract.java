package com.mechaniciancombat.client;

/** Exercises boss camera overrides with and without ShoulderSurfing. */
public final class GunPerspectiveGuardContract {
    public static void main(String[] args) {
        var plain = new FakeCamera(false);
        var plainGuard = new GunPerspectiveGuard<String>();
        plainGuard.update(false, plain);
        check(plain.current.equals("first"), "non-gun view must be left alone");
        plainGuard.update(true, plain);
        check(plain.current.equals("back"), "equipping a gun must enter third-person back");
        plain.current = "first";
        plainGuard.update(true, plain);
        check(plain.current.equals("back"), "boss first-person change must be corrected");
        plainGuard.update(false, plain);
        check(plain.current.equals("first"), "unequipping must restore the original view");

        var shoulder = new FakeCamera(true);
        var shoulderGuard = new GunPerspectiveGuard<String>();
        shoulderGuard.update(true, shoulder);
        check(shoulder.shoulder && shoulder.current.equals("back"), "shoulder view must activate");
        shoulder.current = "first"; shoulder.shoulder = false;
        shoulderGuard.update(true, shoulder);
        check(shoulder.shoulder && shoulder.current.equals("back"), "shoulder view must recover after a boss override");
        check(shoulder.enterCalls == 1, "recovery must not overwrite the pre-gun shoulder perspective");
        shoulder.current = "first"; shoulder.shoulder = false; shoulder.failEnsure = true;
        shoulderGuard.update(true, shoulder);
        check(shoulder.current.equals("back"), "failed shoulder recovery must still use third person");
        shoulder.failEnsure = false;
        shoulderGuard.update(true, shoulder);
        check(shoulder.shoulder, "shoulder recovery must be retried");
        shoulderGuard.update(false, shoulder);
        check(shoulder.current.equals("first"), "shoulder recovery must retain the pre-gun view");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static final class FakeCamera implements GunPerspectiveGuard.Camera<String> {
        final boolean hasShoulder;
        String current = "first";
        boolean shoulder;
        boolean failEnsure;
        int enterCalls;
        FakeCamera(boolean hasShoulder) { this.hasShoulder = hasShoulder; }
        public String current() { return current; }
        public boolean enterShoulder() { enterCalls++; if (hasShoulder) { shoulder=true; current="back"; } return hasShoulder; }
        public boolean isShoulder() { return shoulder; }
        public boolean ensureShoulder() { if (failEnsure) return false; shoulder=true; current="back"; return true; }
        public boolean isBack() { return current.equals("back"); }
        public void useBack() { current="back"; }
        public void leaveShoulder() { shoulder=false; }
        public void restore(String prior) { current=prior; }
        public void onRelease() { }
    }
}
