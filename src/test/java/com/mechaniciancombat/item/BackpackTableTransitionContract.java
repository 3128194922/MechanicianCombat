package com.mechaniciancombat.item;

/** Plain-JDK regression checks for backpack attachment transitions. */
public final class BackpackTableTransitionContract {
	public static void main(String[] args) {
		check(BackpackTableTransition.shouldDetachPreviousBackpack(true, true, false),
				"swapping to a different backpack must detach the previous modules");
		check(!BackpackTableTransition.shouldDetachPreviousBackpack(true, true, true),
				"an unchanged backpack must keep its installed modules attached");
		check(BackpackTableTransition.shouldDetachPreviousBackpack(true, false, false),
				"removing a backpack must detach its installed modules");
		check(!BackpackTableTransition.shouldDetachPreviousBackpack(false, true, false),
				"inserting the first backpack must not detach unrelated modules");
		com.mechaniciancombat.client.rig.ArmorPartPolicyContract.main(args);
		AmmoRingRulesContract.main(args);
		com.mechaniciancombat.client.ShoulderCameraAnglesContract.main(args);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private BackpackTableTransitionContract() {
	}
}
