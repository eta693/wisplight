package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WispOrbEntity;
import fr.leho.wisplight.entity.WispVariant;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class WispOrbRenderer extends EntityRenderer<WispOrbEntity> {
    private static final ResourceLocation PLAIN = Wisplight.id("textures/entity/wisp_orb/plain.png");

    public WispOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WispOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        SpriteQuad.draw(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
                LightTexture.FULL_BRIGHT, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WispOrbEntity entity) {
        WispVariant v = entity.getOrbVariant();
        return v == null ? PLAIN : Wisplight.id("textures/entity/wisp_orb/" + v.id() + ".png");
    }

    @Override
    protected int getBlockLightLevel(WispOrbEntity entity, BlockPos pos) {
        return 15;
    }
}
