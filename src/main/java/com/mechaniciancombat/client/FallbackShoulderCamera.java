package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;

/**
 * Built-in shoulder camera used only when ShoulderSurfing is absent.
 *
 * <p>The camera owns a separate yaw/pitch pair. Mouse deltas update those
 * angles instead of the player's rotation, while {@link GunCamera} decides
 * when the body should follow the camera or track the crosshair.
 */
public final class FallbackShoulderCamera {
	private static final boolean SHOULDER_SURFING_INSTALLED =
			net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing");
	private static final double DEFAULT_DISTANCE = 3.2D;
	private static final double DEFAULT_SIDE = 0.68D;
	private static final double DEFAULT_HEIGHT = 0.18D;
	private static final double AIM_DISTANCE = 2.35D;
	private static final double AIM_SIDE = 0.52D;
	private static final double AIM_HEIGHT = 0.12D;
	private static final double ACTION_DISTANCE = 3.55D;
	private static final double ACTION_SIDE = 0.58D;
	private static final double ACTION_HEIGHT = 0.32D;
	private static final double TRANSITION_SECONDS = 0.10D;
	private static final double COLLISION_MARGIN = 0.15D;

	private static boolean active;
	private static boolean initialized;
	private static final ShoulderCameraAngles ANGLES = new ShoulderCameraAngles(0, 0);
	private static LocalPlayer cameraPlayer;
	private static double distance = DEFAULT_DISTANCE;
	private static double side = DEFAULT_SIDE;
	private static double height = DEFAULT_HEIGHT;

	public record Pose(Vec3 position, float yaw, float pitch) {
	}

	public static boolean isFallbackActive(Minecraft client) {
		return active && isEligible(client);
	}

	public static boolean usesFallback(Minecraft client) {
		return !SHOULDER_SURFING_INSTALLED && isEligible(client);
	}

	private static boolean isEligible(Minecraft client) {
		return client != null
				&& client.player != null
				&& client.player.isAlive() && !client.player.isSpectator()
				&& TkGun.holdingGun(client.player)
				&& client.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
	}

	public static void tick(Minecraft client) {
		if (!usesFallback(client)) {
			if (active) {
				leave(client);
			}
			return;
		}
		if (cameraPlayer != client.player) { active = false; initialized = false; }
		if (!initialized) {
			cameraPlayer = client.player;
			ANGLES.reset(client.player.getYRot(), client.player.getXRot());
			distance = DEFAULT_DISTANCE;
			side = DEFAULT_SIDE;
			height = DEFAULT_HEIGHT;
			initialized = true;
		}
		active = true;

		double targetDistance = DEFAULT_DISTANCE;
		double targetSide = DEFAULT_SIDE;
		double targetHeight = DEFAULT_HEIGHT;
		boolean aiming = GunCamera.aiming(client);
		boolean focusing = GunClientState.focusish(client.player.getId());
		GunClientState.Entry state = GunClientState.get(client.player.getId());
		boolean action = state != null && (state.state == TkGun.State.ROLL
				|| state.state == TkGun.State.ROLL_TO_IDLE
				|| state.state == TkGun.State.SLIDE
				|| state.state == TkGun.State.SLIDE_READY);
		if (action) {
			targetDistance = ACTION_DISTANCE;
			targetSide = ACTION_SIDE;
			targetHeight = ACTION_HEIGHT;
		} else if (aiming || focusing) {
			targetDistance = AIM_DISTANCE;
			targetSide = AIM_SIDE;
			targetHeight = AIM_HEIGHT;
		}

		double frameDelta = Math.min(0.25D,
				Math.max(0.0D, client.getDeltaFrameTime() / 20.0D));
		double blend = 1.0D - Math.exp(-frameDelta / TRANSITION_SECONDS);
		distance += (targetDistance - distance) * blend;
		side += (targetSide - side) * blend;
		height += (targetHeight - height) * blend;
	}

	/** Applies mouse deltas to the fallback camera and prevents vanilla body rotation. */
	public static boolean turn(LocalPlayer player, double deltaX, double deltaY) {
		Minecraft client = Minecraft.getInstance();
		if (!active || !initialized || client.player != player || !isEligible(client) || client.screen != null) {
			return false;
		}
		ANGLES.turn(deltaX, deltaY);
		player.setYHeadRot(ANGLES.yaw());
		player.setXRot(ANGLES.pitch());
		if (ShoulderCouplingState.shouldCoupleCamera(client)) {
			player.setYRot(ANGLES.yaw());
			player.setYBodyRot(ANGLES.yaw());
		}
		return true;
	}

	/**
	 * Align running movement with the camera while preserving all movement input.
	 */
	public static boolean steerRunningInput(Minecraft client, KeyboardInput input) {
		if (!active || !isEligible(client) || client.screen != null) {
			return false;
		}
		LocalPlayer player = client.player;
		boolean dashing = GunClientState.dashing(player.getId());
		boolean sprintIntent = player.isSprinting()
				|| (client.options.keySprint.isDown() && input.forwardImpulse > 0.05F);
		if (!dashing && !sprintIntent) {
			return false;
		}

		float steer = Mth.clamp(input.leftImpulse, -1.0F, 1.0F);
		float yBodyRot = ShoulderCameraAngles.steerYaw(player.getYRot(), ANGLES.yaw(), steer);
		player.setYRot(yBodyRot);
		player.setYBodyRot(yBodyRot);
		player.setYHeadRot(ANGLES.yaw());

		return true;
	}

	public static Pose computePose(BlockGetter view, Entity focusedEntity, float tickDelta) {
		if (!active || !initialized || !(focusedEntity instanceof LocalPlayer player)) {
			return null;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player != player || !isEligible(client)) {
			return null;
		}

		Vec3 look = Vec3.directionFromRotation(ANGLES.pitch(), ANGLES.yaw());
		double yawRadians = Math.toRadians(ANGLES.yaw());
		Vec3 right = new Vec3(-Math.cos(yawRadians), 0.0D, -Math.sin(yawRadians));
		Vec3 interpolated = player.getPosition(tickDelta);
		Vec3 eye = interpolated.add(0.0D, player.getEyeHeight(player.getPose()), 0.0D);
		Vec3 desired = eye
				.subtract(look.scale(distance))
				.add(right.scale(side))
				.add(0.0D, height, 0.0D);

		Vec3 delta = desired.subtract(eye);
		double desiredDistance = delta.length();
		if (desiredDistance > 1.0E-5D) {
			BlockHitResult hit = view.clip(new ClipContext(
					eye, desired, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, player));
			if (hit.getType() != HitResult.Type.MISS) {
				double safeDistance = Math.max(0.0D,
						eye.distanceTo(hit.getLocation()) - COLLISION_MARGIN);
				desired = eye.add(delta.normalize().scale(safeDistance));
			}
		}

		return new Pose(desired, ANGLES.yaw(), ANGLES.pitch());
	}

	public static void leave(Minecraft client) {
		if (active && initialized && client.player != null && client.player == cameraPlayer) {
			// Preserve the visible camera direction when returning to vanilla controls.
			client.player.setYRot(ANGLES.yaw());
			client.player.setXRot(ANGLES.pitch());
			client.player.setYHeadRot(ANGLES.yaw());
			client.player.setYBodyRot(ANGLES.yaw());
			client.player.yRotO = ANGLES.yaw();
			client.player.xRotO = ANGLES.pitch();
		}
		active = false;
		initialized = false;
		cameraPlayer = null;
	}

	public static float cameraYaw() {
		return ANGLES.yaw();
	}

	public static float cameraPitch() {
		return ANGLES.pitch();
	}

	private FallbackShoulderCamera() {
	}
}
