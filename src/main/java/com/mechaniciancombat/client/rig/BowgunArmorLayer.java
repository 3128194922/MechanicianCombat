package com.mechaniciancombat.client.rig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

/**
 * 把原版玩家盔甲画在 GeckoLib rig 的身体骨骼上。
 *
 * <p>本模组在手持破垒重炮时取消的是**整条 PlayerEntityRenderer**，原版
 * {@code ArmorFeatureRenderer} 也跟着不执行。这里不能再拿原版 PlayerEntityModel 近似 rig 姿势
 * （那会造成之前出现过的肢体变形），而是在每一根身体骨骼自己的 poseStack 里，把该骨骼的方块
 * 按盔甲贴图再画一遍：
 *
 * <ul>
 *   <li>头盔 → {@code head}</li>
 *   <li>胸甲 → {@code chest} + 两条手臂</li>
 *   <li>护腿 → {@code chest}（腰带部分）+ 两条腿</li>
 *   <li>靴子 → 两条腿</li>
 * </ul>
 *
	 * <p>玩家的 64x64 皮肤和 64x32 盔甲纹理使用同一套玩家模型**像素坐标**，但归一化后的 V
	 * 要按 64x32 重新换算（纵向乘 2），否则所有面都会采样到错误的盔甲区域。
	 * 几何不用重新生成，只按原版 {@code Dilation} 的规则把方块整体向六个方向各扩 1px
	 *（护腿内层 0.5px），再换
	 * {@code RenderLayer.getArmorCutoutNoCull(texture)}。
 * 染色、多材质层、盔甲镶饰和附魔光效都按原版 ArmorFeatureRenderer 的数据路径处理。
 *
 * <p>可关闭：启动参数 {@code -Dtkgun.armorlayer=false}。
 */
public final class BowgunArmorLayer extends GeoRenderLayer<BowgunRig> {
	private static final boolean ENABLED =
			!"false".equalsIgnoreCase(System.getProperty("tkgun.armorlayer", "true"));
	private static final net.minecraft.util.RandomSource REFERENCE_CUBE_RANDOM = net.minecraft.util.RandomSource.create(0L);
	private HumanoidModel<LivingEntity> innerArmorModel;
	private HumanoidModel<LivingEntity> outerArmorModel;

	public BowgunArmorLayer(GeoRenderer<BowgunRig> renderer) {
		super(renderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, BowgunRig animatable, GeoBone bone,
							  RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
							  float partialTick, int packedLight, int packedOverlay) {
		if (!ENABLED || bone.getCubes().isEmpty()) {
			return;
		}
		if (!(this.renderer instanceof BowgunRigRenderer rig)) {
			return;
		}
		Object current = rig.getCurrentEntity();
		if (!(current instanceof LivingEntity living)) {
			return;
		}
		List<EquipmentSlot> slots = slotsFor(bone.getName());
		if (slots.isEmpty()) {
			return;
		}
		for (EquipmentSlot slot : slots) {
			ItemStack stack = living.getItemBySlot(slot);
			if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
				continue;
			}
			ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
			if (itemId != null && (itemId.getNamespace().equals("caverns_and_chasms")
					|| itemId.getNamespace().equals("oreganized"))
					&& renderReferenceArmor(poseStack, bone, living, stack, armor, slot, buffers, packedLight, packedOverlay)) {
				continue;
			}
			boolean inner = slot == EquipmentSlot.LEGS;
			float inflate = (inner ? 0.5F : 1.0F) / 16.0F;
			int color = stack.getItem() instanceof DyeableLeatherItem dyeable
					? (dyeable.getColor(stack) | 0xFF000000)
					: -1;
			ArmorMaterial material = armor.getMaterial();
			ResourceLocation materialId = ResourceLocation.tryParse(material.getName());
			if (materialId != null) {
				ResourceLocation texture = new ResourceLocation(materialId.getNamespace(),
						"textures/models/armor/" + materialId.getPath()
								+ "_layer_" + (inner ? 2 : 1) + ".png");
				renderCubes(poseStack, bone, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)),
						packedLight, packedOverlay, color, inflate);
			}
			renderTrim(poseStack, bone, stack, armor, inner, buffers, packedLight, packedOverlay, inflate);
			if (stack.isEnchanted()) {
				renderCubes(poseStack, bone, buffers.getBuffer(RenderType.armorEntityGlint()),
						packedLight, packedOverlay, -1, inflate);
			}
		}
	}

	/** Uses the armor item's Forge model and texture for the two supported mods. */
	private boolean renderReferenceArmor(PoseStack poseStack, GeoBone bone, LivingEntity wearer,
			ItemStack stack, ArmorItem armor, EquipmentSlot slot, MultiBufferSource buffers,
			int light, int overlay) {
		boolean inner = slot == EquipmentSlot.LEGS;
		HumanoidModel<LivingEntity> base = inner ? innerArmorModel : outerArmorModel;
		if (base == null) {
			base = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
					.bakeLayer(inner ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR));
			if (inner) innerArmorModel = base; else outerArmorModel = base;
		}
		HumanoidModel<?> model = IClientItemExtensions.of(stack).getHumanoidArmorModel(wearer, stack, slot, base);
		String modelName = model.getClass().getName();
		String kind = modelName.equals("galena.oreganized.client.model.ElectrumArmorModel") ? "electrum"
				: modelName.equals("com.teamabnormals.caverns_and_chasms.client.model.ToolbeltModel") ? "toolbelt" : "humanoid";
		String name = ArmorPartPolicy.part(kind, bone.getName(), slot.name());
		if (name == null) return true;
		ModelPart part = modelPart(model, name);
		ModelPart reference = modelPart(base, ArmorPartPolicy.part("humanoid", bone.getName(), slot.name()));
		if (part == null || reference == null || reference.isEmpty()) return false;
		ModelPart.Cube cube = reference.getRandomCube(REFERENCE_CUBE_RANDOM);
		GeoCube geo = bone.getCubes().get(0);
		float sx = (float)(geo.size().x() / Math.abs(cube.maxX - cube.minX));
		float sy = (float)(geo.size().y() / Math.abs(cube.maxY - cube.minY));
		float sz = (float)(geo.size().z() / Math.abs(cube.maxZ - cube.minZ));
		if (sx == 0 || sy == 0 || sz == 0) return false;
		PartPose saved = part.storePose();
		boolean wasVisible = part.visible;
		poseStack.pushPose();
		try {
			poseStack.scale(-1, -1, 1);
			poseStack.scale(sx, sy, sz);
			part.setPos(-bone.getPivotX() / sx, -bone.getPivotY() / sy, bone.getPivotZ() / sz);
			part.xRot = -bone.getRotX();
			part.yRot = -bone.getRotY();
			part.zRot = bone.getRotZ();
			part.visible = true;
			ResourceLocation mat = ResourceLocation.tryParse(armor.getMaterial().getName());
			if (mat == null) return false;
			String defaultTexture = mat.getNamespace() + ":textures/models/armor/" + mat.getPath()
					+ "_layer_" + (inner ? 2 : 1) + ".png";
			String texture = ForgeHooksClient.getArmorTexture(wearer, stack, defaultTexture, slot, null);
			int color = stack.getItem() instanceof DyeableLeatherItem dye ? dye.getColor(stack) : 0xffffff;
			VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffers,
					RenderType.armorCutoutNoCull(new ResourceLocation(texture)), false, stack.hasFoil());
			part.render(poseStack, consumer, light, overlay, (color >> 16 & 255) / 255f,
					(color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
			if (stack.getItem() instanceof DyeableLeatherItem) {
				String overlayDefault = defaultTexture.substring(0, defaultTexture.length() - 4) + "_overlay.png";
				String overlayTexture = ForgeHooksClient.getArmorTexture(wearer, stack, overlayDefault, slot, "overlay");
				part.render(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(new ResourceLocation(overlayTexture))), light, overlay);
			}
			if (Minecraft.getInstance().level != null) {
				ArmorTrim trim = ArmorTrim.getTrim(Minecraft.getInstance().level.registryAccess(), stack).orElse(null);
				if (trim != null) {
					ResourceLocation trimId = inner ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
					TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(trimId);
					part.render(poseStack, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet())), light, overlay);
				}
			}
			return true;
		} finally {
			part.loadPose(saved);
			part.visible = wasVisible;
			poseStack.popPose();
		}
	}

	private static ModelPart modelPart(HumanoidModel<?> model, String name) {
		if (name == null) return null;
		return switch (name) {
			case "head" -> model.head;
			case "body" -> model.body;
			case "leftArm" -> model.leftArm;
			case "rightArm" -> model.rightArm;
			case "leftLeg" -> model.leftLeg;
			case "rightLeg" -> model.rightLeg;
			default -> {
				try {
					var field = model.getClass().getDeclaredField(name);
					field.setAccessible(true);
					yield (ModelPart) field.get(model);
				} catch (ReflectiveOperationException | ClassCastException e) {
					yield null;
				}
			}
		};
	}

	/** 一根 rig 骨骼需要承接哪些装备槽（护腿和靴子会共用腿骨）。 */
	private static List<EquipmentSlot> slotsFor(String bone) {
		return switch (bone) {
			case "head" -> List.of(EquipmentSlot.HEAD);
			case "chest" -> List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS);
			case "arm_left", "arm_right" -> List.of(EquipmentSlot.CHEST);
			case "leg_left", "leg_right" -> List.of(EquipmentSlot.LEGS, EquipmentSlot.FEET);
			default -> List.of();
		};
	}

	private void renderTrim(PoseStack poseStack, GeoBone bone, ItemStack stack, ArmorItem armor,
							boolean inner, MultiBufferSource buffers, int packedLight,
							int packedOverlay, float inflate) {
		ArmorTrim trim = ArmorTrim.getTrim(Minecraft.getInstance().level.registryAccess(), stack).orElse(null);
		if (trim == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.getModelManager() == null) {
			return;
		}
		ResourceLocation modelId = inner
				? trim.innerTexture(armor.getMaterial())
				: trim.outerTexture(armor.getMaterial());
		TextureAtlasSprite sprite = client.getModelManager()
				.getAtlas(Sheets.ARMOR_TRIMS_SHEET)
				.getSprite(modelId);
		RenderType layer = Sheets.armorTrimsSheet();
		VertexConsumer trimBuffer = sprite.wrap(
				buffers.getBuffer(layer));
		renderCubes(poseStack, bone, trimBuffer, packedLight, packedOverlay, -1, inflate);
	}

	private void renderCubes(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
							 int packedLight, int packedOverlay, int color, float inflate) {
		if (!(this.renderer instanceof BowgunRigRenderer rig)) {
			return;
		}
		GeoBone sourceBone = bone;
		String mirrorSource = switch (bone.getName()) {
			case "arm_left" -> "arm_right";
			case "leg_left" -> "leg_right";
			default -> null;
		};
		if (mirrorSource != null) {
			sourceBone = rig.getGeoModel().getBone(mirrorSource).orElse(null);
			if (sourceBone == null) {
				return;
			}
		}
		for (GeoCube cube : sourceBone.getCubes()) {
			GeoCube shaped = mirrorSource != null ? mirrorX(cube) : cube;
			this.renderer.renderCube(poseStack, dilate(shaped, inflate), buffer,
					packedLight, packedOverlay,
					((color >> 16) & 255) / 255.0F,
					((color >> 8) & 255) / 255.0F,
					(color & 255) / 255.0F,
					((color >>> 24) & 255) / 255.0F);
		}
	}

	/**
	 * 按原版 {@code Dilation} 的规则扩张方块：每条轴的 min/max 都向两侧各移 1px，
	 * 相邻面仍然共享顶点，不会像“逐面沿法线平移”那样在六个棱边留下透明缝隙。
	 * UV 仍覆盖完整的原版盔甲面。
	 */
	private static GeoCube dilate(GeoCube cube, float amount) {
		GeoQuad[] quads = cube.quads();
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float maxZ = Float.NEGATIVE_INFINITY;
		for (GeoQuad quad : quads) {
			if (quad == null) {
				continue;
			}
			for (GeoVertex vertex : quad.vertices()) {
				Vector3f p = vertex.position();
				minX = Math.min(minX, p.x());
				minY = Math.min(minY, p.y());
				minZ = Math.min(minZ, p.z());
				maxX = Math.max(maxX, p.x());
				maxY = Math.max(maxY, p.y());
				maxZ = Math.max(maxZ, p.z());
			}
		}
		float centerX = (minX + maxX) * 0.5F;
		float centerY = (minY + maxY) * 0.5F;
		float centerZ = (minZ + maxZ) * 0.5F;
		GeoQuad[] out = new GeoQuad[quads.length];
		for (int i = 0; i < quads.length; i++) {
			GeoQuad quad = quads[i];
			if (quad == null) {
				continue;
			}
			GeoVertex[] vertices = quad.vertices();
			GeoVertex[] moved = new GeoVertex[vertices.length];
			for (int j = 0; j < vertices.length; j++) {
				GeoVertex v = vertices[j];
				Vector3f p = new Vector3f(v.position());
				p.x = p.x < centerX ? p.x - amount : p.x + amount;
				p.y = p.y < centerY ? p.y - amount : p.y + amount;
				p.z = p.z < centerZ ? p.z - amount : p.z + amount;
				moved[j] = new GeoVertex(
						p,
						v.texU(), v.texV() * 2.0F);
			}
			out[i] = new GeoQuad(moved, quad.normal(), quad.direction());
		}
		return new GeoCube(out, cube.pivot(), cube.rotation(), cube.size(),
				cube.inflate(), cube.mirror());
	}

	/**
	 * 取右臂/右腿的 UV 和几何，沿模型 X=0 镜像后交给左骨骼。
	 *
	 * <p>皮肤左侧区域（32..44 / 16..28，y48..60）和原版盔甲贴图不是同一套面排列，
	 * 只做 U 翻转会串面。原版 ArmorEntityModel 的做法就是复用右侧 cuboid 的 UV，
	 * 以 {@code mirrored=true} 生成左侧几何；这里等价地镜像顶点、法线和 pivot。
	 */
	private static GeoCube mirrorX(GeoCube cube) {
		GeoQuad[] quads = cube.quads();
		GeoQuad[] out = new GeoQuad[quads.length];
		for (int i = 0; i < quads.length; i++) {
			GeoQuad quad = quads[i];
			if (quad == null) {
				continue;
			}
			GeoVertex[] vertices = quad.vertices();
			GeoVertex[] mirrored = new GeoVertex[vertices.length];
			for (int j = 0; j < vertices.length; j++) {
				GeoVertex v = vertices[j];
				Vector3f p = new Vector3f(v.position());
				p.x = -p.x;
				mirrored[j] = new GeoVertex(p, v.texU(), v.texV());
			}
			Vector3f normal = new Vector3f(quad.normal());
			normal.x = -normal.x;
			Direction direction = switch (quad.direction()) {
				case EAST -> Direction.WEST;
				case WEST -> Direction.EAST;
				default -> quad.direction();
			};
			out[i] = new GeoQuad(mirrored, normal, direction);
		}
		Vec3 pivot = cube.pivot();
		return new GeoCube(out, new Vec3(-pivot.x, pivot.y, pivot.z),
				cube.rotation(), cube.size(), cube.inflate(), cube.mirror());
	}
}
