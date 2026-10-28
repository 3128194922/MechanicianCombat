package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;

public final class ClientAimStateContract {
    public static void main(String[] args) {
        GunClientState.Entry entry = new GunClientState.Entry();
        entry.state = TkGun.State.ROLL;
        entry.dashing = true;
        entry.dashUntil = 100;
        check(GunClientState.aimLocked(entry, 10), "actual roll must block aim");
        entry.state = TkGun.State.ROLL_TO_IDLE;
        check(!GunClientState.aimLocked(entry, 10), "roll recovery must allow aim during dash");
        entry.state = TkGun.State.IDLE;
        check(!GunClientState.aimLocked(entry, 10), "dash after roll must not block aim");
        entry.state = TkGun.State.SLIDE;
        check(GunClientState.aimLocked(entry, 10), "slide must block aim");
        entry.state = TkGun.State.IDLE;
        entry.aimLockUntil = 30;
        check(GunClientState.aimLocked(entry, 10), "slide recovery lock must block aim");
        entry.aimLockUntil = 0;
        entry.heatLockUntil = 30;
        check(GunClientState.aimLocked(entry, 10), "overheat cooling must block aim");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
