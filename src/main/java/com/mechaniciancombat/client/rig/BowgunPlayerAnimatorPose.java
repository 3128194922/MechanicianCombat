package com.mechaniciancombat.client.rig;

import com.mechaniciancombat.TkGun;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bridges the currently evaluated GeckoLib body pose into PlayerAnimator's vanilla player model. */
public final class BowgunPlayerAnimatorPose implements IAnimation {
	private static final int LAYER_PRIORITY = 20;
	private static final boolean TRACE_POSE_BRIDGE = System.getProperty("tkgun.posebridge") != null;
	private static final Set<String> TRACED_PARTS = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, PoseSnapshot> POSES = new ConcurrentHashMap<>();
	private static net.minecraft.client.multiplayer.ClientLevel cachedLevel;
	private static long lastPruneGameTime = Long.MIN_VALUE;
	private static Map<String, Set<String>> animationChannels;

	private final AbstractClientPlayer player;

	private BowgunPlayerAnimatorPose(AbstractClientPlayer player) {
		this.player = player;
	}

	/** Register one live bridge on each player when PlayerAnimator creates its animation stack. */
	public static void register() {
		if (!ModList.get().isLoaded("playeranimator")) {
			TkGun.LOGGER.error("[tkgun] PlayerAnimator 未加载，无法启用原版玩家枪械动画");
			return;
		}
		PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(
				(player, stack) -> stack.addAnimLayer(LAYER_PRIORITY, new BowgunPlayerAnimatorPose(player)));
		TkGun.LOGGER.info("[tkgun] 已注册 PlayerAnimator 原版玩家姿势桥接");
	}

	public static void capture(int playerId, Map<String, BonePose> pose) {
		Minecraft client = Minecraft.getInstance();
		if (cachedLevel != client.level) {
			POSES.clear();
			cachedLevel = client.level;
			lastPruneGameTime = Long.MIN_VALUE;
		}
		long gameTime = client.level == null ? 0L : client.level.getGameTime();
		if (gameTime != lastPruneGameTime) {
			lastPruneGameTime = gameTime;
			POSES.entrySet().removeIf(entry -> gameTime - entry.getValue().gameTime() > 2L);
		}
		Set<String> activeAnimations = BowgunRig.currentPlayerPoseAnimations();
		Set<String> channels = new HashSet<>();
		Map<String, Set<String>> knownChannels = channels();
		for (String animation : activeAnimations) channels.addAll(knownChannels.getOrDefault(animation, Set.of()));
		BonePose chest = pose.get("chest");
		// walk_* 的腰射胸部起伏由渲染器额外叠加，不属于原动画 JSON 的通道表。
		if (chest != null && chest.y() != 0.0F) channels.add("chest.position");
		POSES.put(playerId, new PoseSnapshot(Map.copyOf(pose), Set.copyOf(channels), gameTime));
	}

	public static void clear() {
		POSES.clear();
		cachedLevel = null;
		lastPruneGameTime = Long.MIN_VALUE;
	}

	@Override
	public boolean isActive() {
		return player.isAlive() && TkGun.holdingGun(player);
	}

	@Override
	public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f fallback) {
		if (!isActive()) {
			return fallback;
		}
		PoseSnapshot snapshot = POSES.get(player.getId());
		if (snapshot == null || player.level().getGameTime() - snapshot.gameTime() > 2L) {
			return fallback;
		}

		Map<String, BonePose> bones = snapshot.bones();
		String[] path = switch (part) {
			case "torso" -> new String[] {"body", "chest"};
			case "head" -> new String[] {"body", "chest", "head"};
			case "leftArm" -> new String[] {"body", "chest", "arm_left"};
			case "rightArm" -> new String[] {"body", "chest", "arm_right"};
			case "leftLeg" -> new String[] {"body", "leg_left"};
			case "rightLeg" -> new String[] {"body", "leg_right"};
			default -> null;
		};
		if (path == null) return fallback;
		tracePoseBridge(snapshot, path, part, type);
		return transform(path, bones, snapshot.channels(), type, fallback);
	}

	static Vec3f transform(String[] path, Map<String, BonePose> bones, Set<String> channels,
						   TransformType type, Vec3f fallback) {
		if (type == TransformType.ROTATION) {
			Quaternionf combined = new Quaternionf();
			boolean hasRotation = false;
			for (int i = 0; i < path.length - 1; i++) {
				if (channels.contains(path[i] + ".rotation")) {
					combined.mul(playerRotation(bones.getOrDefault(path[i], BonePose.ZERO)));
					hasRotation = true;
				}
			}
			String directName = path[path.length - 1];
			if (channels.contains(directName + ".rotation")) {
				combined.mul(playerRotation(bones.getOrDefault(directName, BonePose.ZERO)));
				hasRotation = true;
			} else {
				combined.mul(rotation(fallback));
			}
			return hasRotation ? eulerZYX(combined.normalize()) : fallback;
		}
		if (type != TransformType.POSITION) return fallback;

		boolean hasPositionEffect = false;
		Matrix4f transform = new Matrix4f();
		for (int i = 0; i < path.length; i++) {
			String name = path[i];
			BonePose pose = bones.getOrDefault(name, BonePose.ZERO);
			boolean hasPosition = channels.contains(name + ".position");
			boolean hasRotation = channels.contains(name + ".rotation");
			if (hasPosition) {
				transform.translate(localTranslation(pose));
				hasPositionEffect = true;
			}
			Vector3f pivot = pivot(pose);
			transform.translate(pivot);
			if (hasRotation) {
				transform.rotate(rotation(pose));
				hasPositionEffect = true;
			}
			transform.translate(new Vector3f(pivot).negate());
		}
		if (!hasPositionEffect) return fallback;

		// Vanilla's part pivot can differ from the corresponding geo bone pivot (e.g. arms).
		// Transform the actual model-part pivot: vanilla y=0 is geo y=24 pixels.
		Vector3f targetPivot = new Vector3f(-fallback.getX(), 24.0F - fallback.getY(), fallback.getZ());
		Vector3f movedPivot = transform.transformPosition(new Vector3f(targetPivot));
		Vector3f delta = movedPivot.sub(targetPivot);
		// PlayerRenderer flips X and Y before rendering ModelPart; GeckoLib's rig does not.
		return new Vec3f(fallback.getX() - delta.x, fallback.getY() - delta.y, fallback.getZ() + delta.z);
	}

	/** GeckoLib applies local translations as (-X, +Y, +Z); preserve that convention in PlayerModel space. */
	private static Vector3f localTranslation(BonePose pose) {
		return new Vector3f(-pose.x(), pose.y(), pose.z());
	}

	private void tracePoseBridge(PoseSnapshot snapshot, String[] path, String part, TransformType type) {
		if (!TRACE_POSE_BRIDGE) return;
		String key = player.getId() + ":" + part + ":" + type;
		if (!TRACED_PARTS.add(key)) return;
		Set<String> activeChannels = new HashSet<>();
		for (String bone : path) {
			for (String channel : Set.of("position", "rotation")) {
				String name = bone + "." + channel;
				if (snapshot.channels().contains(name)) activeChannels.add(name);
			}
		}
		TkGun.LOGGER.info("[tkgun] PlayerAnimator 姿势回调：player={} part={} type={} channels={}",
				player.getId(), part, type, activeChannels);
	}

	private static Vector3f pivot(BonePose pose) {
		return new Vector3f(pose.pivotX(), pose.pivotY(), pose.pivotZ());
	}

	/** GeckoLib rotates each bone Z, then Y, then X, so compose the same local transform order. */
	private static Quaternionf rotation(BonePose pose) {
		return new Quaternionf().rotationZYX(pose.rotZ(), pose.rotY(), pose.rotX());
	}

	/** Change basis from the GeckoLib model frame to the vanilla player model frame. */
	private static Quaternionf playerRotation(BonePose pose) {
		return new Quaternionf().rotationZYX(pose.rotZ(), -pose.rotY(), -pose.rotX());
	}

	private static Quaternionf rotation(Vec3f pose) {
		return new Quaternionf().rotationZYX(pose.getZ(), pose.getY(), pose.getX());
	}

	private static Vec3f eulerZYX(Quaternionf q) {
		float x = (float) Math.atan2(2.0F * (q.w * q.x + q.y * q.z), 1.0F - 2.0F * (q.x * q.x + q.y * q.y));
		float y = (float) Math.asin(Math.max(-1.0F, Math.min(1.0F, 2.0F * (q.w * q.y - q.z * q.x))));
		float z = (float) Math.atan2(2.0F * (q.w * q.z + q.x * q.y), 1.0F - 2.0F * (q.y * q.y + q.z * q.z));
		return new Vec3f(x, y, z);
	}

	private static synchronized Map<String, Set<String>> channels() {
		if (animationChannels != null) return animationChannels;
		Map<String, Set<String>> parsed = new HashMap<>();
		ResourceLocation resourceId = new ResourceLocation(TkGun.MOD_ID, "animations/entity/bowgun_rig.animation.json");
		try (var resource = Minecraft.getInstance().getResourceManager().getResource(resourceId).orElseThrow().open();
				var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
			JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
			for (Map.Entry<String, JsonElement> animation : animations.entrySet()) {
				Set<String> keys = new HashSet<>();
				JsonObject bones = animation.getValue().getAsJsonObject().getAsJsonObject("bones");
				if (bones != null) {
					for (Map.Entry<String, JsonElement> bone : bones.entrySet()) {
						JsonObject boneChannels = bone.getValue().getAsJsonObject();
						for (String channel : Set.of("position", "rotation")) {
							if (boneChannels.has(channel)) keys.add(bone.getKey() + "." + channel);
						}
					}
				}
				parsed.put(animation.getKey(), Set.copyOf(keys));
			}
		} catch (Exception exception) {
			TkGun.LOGGER.error("[tkgun] 读取 PlayerAnimator 姿势通道失败：{}", resourceId, exception);
		}
		animationChannels = Map.copyOf(parsed);
		return animationChannels;
	}

	@Override
	public void setupAnim(float tickDelta) {
		// The GeckoLib rig and PlayerAnimator both use client render partial ticks.
	}

	public record BonePose(float x, float y, float z, float rotX, float rotY, float rotZ,
						float pivotX, float pivotY, float pivotZ) {
		private static final BonePose ZERO = new BonePose(0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	private record PoseSnapshot(Map<String, BonePose> bones, Set<String> channels, long gameTime) {
	}
	private BowgunPlayerAnimatorPose() {
		throw new AssertionError();
	}
}
