package com.mechaniciancombat.client.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

/** Align a vanilla model part with the rig cube in the bone's model coordinates. */
final class ArmorModelAlignment {
    static boolean apply(PoseStack pose, GeoCube geo, ModelPart.Cube reference, ModelPart part) {
        float width = reference.maxX - reference.minX;
        float height = reference.maxY - reference.minY;
        float depth = reference.maxZ - reference.minZ;
        if (width <= 0 || height <= 0 || depth <= 0) return false;
        float sx = (float) geo.size().x() / width;
        float sy = (float) geo.size().y() / height;
        float sz = (float) geo.size().z() / depth;
        if (!Float.isFinite(sx) || !Float.isFinite(sy) || !Float.isFinite(sz)
                || sx <= 0 || sy <= 0 || sz <= 0) return false;
        float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (GeoQuad quad : geo.quads()) {
            if (quad == null) continue;
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f point = vertex.position();
                minX = Math.min(minX, point.x()); maxX = Math.max(maxX, point.x());
                minY = Math.min(minY, point.y()); maxY = Math.max(maxY, point.y());
                minZ = Math.min(minZ, point.z()); maxZ = Math.max(maxZ, point.z());
            }
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) return false;
        float centerX = (minX + maxX) * 8F;
        float centerY = (minY + maxY) * 8F;
        float centerZ = (minZ + maxZ) * 8F;
        float partX = -centerX / sx - (reference.minX + reference.maxX) * 0.5F;
        float partY = -centerY / sy - (reference.minY + reference.maxY) * 0.5F;
        float partZ = centerZ / sz - (reference.minZ + reference.maxZ) * 0.5F;
        pose.scale(-1, -1, 1);
        pose.scale(sx, sy, sz);
        part.setPos(partX, partY, partZ);
        // The pose stack already contains this bone's full animation transform.
        part.setRotation(0, 0, 0);
        return true;
    }
    private ArmorModelAlignment() {}
}
