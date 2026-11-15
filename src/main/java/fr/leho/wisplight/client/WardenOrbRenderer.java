package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WardenOrbEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class WardenOrbRenderer extends EntityRenderer<WardenOrbEntity> {
    private static final ResourceLocation TEXTURE = Wisplight.id("textures/entity/warden_orb.png");

    public WardenOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WardenOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float pulse = 0.75F + Mth.sin((entity.tickCount + partialTick) * 0.5F) * 0.08F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(pulse, pulse, pulse);
        SpriteQuad.draw(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), LightTexture.FULL_BRIGHT, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WardenOrbEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(WardenOrbEntity entity, BlockPos pos) {
        return 15;
    }
}
