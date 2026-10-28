package com.mechaniciancombat.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Checks the actual Forge vertex writer used by the heat-vent particle. */
public final class HeatVentVertexContract {
    public static void main(String[] args) {
        RecordingVertex consumer = new RecordingVertex();
        for (int i = 0; i < 4; i++) {
            HeatVentParticle.writeVertex(consumer, 0x00f000f0, i, 2, 3, 0.2F, 0.7F,
                    1F, 1F, 1F, 0.5F);
        }
        if (consumer.vertices != 4 || consumer.completed != 4 || consumer.open)
            throw new AssertionError("each of the four particle vertices must be completed");
        if (consumer.lights != 4 || consumer.colors != 4 || consumer.uvs != 4)
            throw new AssertionError("each vertex needs color, texture coordinates and light");
        com.mojang.blaze3d.vertex.BufferBuilder buffer = new com.mojang.blaze3d.vertex.BufferBuilder(256);
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        for (int i = 0; i < 4; i++) {
            HeatVentParticle.writeVertex(buffer, 0x00f000f0, i, 2, 3, 0.2F, 0.7F,
                    1F, 1F, 1F, 0.5F);
        }
        var rendered = buffer.end();
        try {
            if (rendered.drawState().vertexCount() != 4)
                throw new AssertionError("particle buffer must contain one complete quad");
        } finally {
            rendered.release();
        }
    }
    private static final class RecordingVertex implements VertexConsumer {
        int vertices, completed, lights, colors, uvs;
        boolean open;
        public VertexConsumer vertex(double x, double y, double z) {
            if (open) throw new AssertionError("previous vertex was not completed");
            open = true; vertices++; return this;
        }
        public VertexConsumer color(int r, int g, int b, int a) { colors++; return this; }
        public VertexConsumer uv(float u, float v) { uvs++; return this; }
        public VertexConsumer uv2(int u, int v) { lights++; return this; }
        public VertexConsumer overlayCoords(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { return this; }
        public void endVertex() { if (!open) throw new AssertionError("no vertex to finish"); open = false; completed++; }
        public void defaultColor(int r, int g, int b, int a) { }
        public void unsetDefaultColor() { }
    }
}
