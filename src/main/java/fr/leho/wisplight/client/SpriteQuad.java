package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Dessine un sprite carré centré sur l'origine, dans le plan XY (taille 1×1). */
public final class SpriteQuad {
    private SpriteQuad() {}

    public static void draw(PoseStack poseStack, VertexConsumer consumer, int light, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, light, alpha, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(consumer, pose, light, alpha, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(consumer, pose, light, alpha, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(consumer, pose, light, alpha, -0.5F, 0.5F, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, int alpha,
                               float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
