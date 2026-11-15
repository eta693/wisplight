package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.WispEntity;
import fr.leho.wisplight.entity.WispVariant;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Rendu du follet : un sprite pixel art toujours tourné vers la caméra, pleinement lumineux. */
public class WispRenderer extends EntityRenderer<WispEntity> {
    private static final Map<WispVariant, ResourceLocation> TEXTURES = Util.make(new EnumMap<>(WispVariant.class), map -> {
        for (WispVariant v : WispVariant.values()) {
            map.put(v, Wisplight.id("textures/entity/wisp/" + v.id() + ".png"));
        }
    });

    public WispRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static ResourceLocation texture(WispVariant variant) {
        return TEXTURES.get(variant);
    }

    @Override
    public void render(WispEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float time = entity.tickCount + partialTick;
        float bob = Mth.sin(time * 0.15F) * 0.06F;
        float pulse = 0.92F + Mth.sin(time * 0.31F) * 0.08F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.3F + bob, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.7F * pulse, 0.7F * pulse, 0.7F * pulse);
        SpriteQuad.draw(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
                LightTexture.FULL_BRIGHT, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WispEntity entity) {
        return texture(entity.getVariant());
    }

    @Override
    protected int getBlockLightLevel(WispEntity entity, BlockPos pos) {
        return 15;
    }
}
