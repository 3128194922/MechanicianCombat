package com.mechaniciancombat.item;

/** Plain-JDK regression checks for server-side ammo-ring input rules. */
public final class AmmoRingRulesContract {
	public static void main(String[] args) {
		check(AmmoRingRules.normalizeSelected(-1) == 7, "negative saved selection must wrap to the last slot");
		check(AmmoRingRules.normalizeSelected(8) == 0, "selection at ring size must wrap to slot zero");
		check(AmmoRingRules.normalizeSelected(Byte.MIN_VALUE) == 0, "signed-byte selection must normalize safely");
		check(AmmoRingRules.nextSelected(0, -1) == 7, "previous input must wrap from the first slot");
		check(AmmoRingRules.nextSelected(7, 1) == 0, "next input must wrap from the last slot");
		check(AmmoRingRules.isValidDirection(-1), "previous direction must be accepted");
		check(AmmoRingRules.isValidDirection(1), "next direction must be accepted");
		check(!AmmoRingRules.isValidDirection(0), "zero direction must be rejected");
		check(!AmmoRingRules.isValidDirection(2), "multi-slot direction must be rejected");
		check(!AmmoRingRules.isValidDirection(Integer.MIN_VALUE), "untrusted extreme direction must be rejected");
		check(AmmoRingRules.isValidConsumeAmount(1), "positive ammo consumption must be accepted");
		check(!AmmoRingRules.isValidConsumeAmount(0), "zero ammo consumption must be rejected");
		check(!AmmoRingRules.isValidConsumeAmount(-1), "negative ammo consumption must not increase ammo");
		check(!AmmoRingRules.isValidConsumeAmount(Integer.MIN_VALUE), "extreme negative consumption must be rejected");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private AmmoRingRulesContract() {
	}
}
