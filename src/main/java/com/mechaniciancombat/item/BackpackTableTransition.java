package com.mechaniciancombat.item;

/** State-transition rules for modules attached through the backpack table. */
public final class BackpackTableTransition {
	public static boolean shouldDetachPreviousBackpack(
			boolean previousBackpackPresent,
			boolean nextBackpackPresent,
			boolean sameStack) {
		return previousBackpackPresent && (!nextBackpackPresent || !sameStack);
	}

	private BackpackTableTransition() {
	}
}
