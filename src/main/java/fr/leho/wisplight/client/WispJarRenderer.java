package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.leho.wisplight.block.WispJarBlock;
import fr.leho.wisplight.blockentity.WispJarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/** Dessine le follet qui flotte dans son bocal ; il grossit un peu quand des essences sont prêtes. */
public class WispJarRenderer implements BlockEntityRenderer<WispJarBlockEntity> {
    public WispJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WispJarBlockEntity jar, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!(jar.getBlockState().getBlock() instanceof WispJarBlock block) || jar.getLevel() == null) {
            return;
        }
        float time = jar.getLevel().getGameTime() + partialTick;
        float bob = Mth.sin(time * 0.12F) * 0.04F;
        float size = 0.36F + 0.025F * jar.getEssences() + Mth.sin(time * 0.3F) * 0.015F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.4F + bob, 0.5F);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(size, size, size);
        SpriteQuad.draw(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(WispRenderer.texture(block.variant()))),
                LightTexture.FULL_BRIGHT, 255);
        poseStack.popPose();
    }
}
