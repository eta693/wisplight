package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.HollowWardenEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class HollowWardenRenderer extends MobRenderer<HollowWardenEntity, HollowWardenModel> {
    private static final ResourceLocation TEXTURE = Wisplight.id("textures/entity/hollow_warden/body.png");
    private static final ResourceLocation[] FLAMES = {
            Wisplight.id("textures/entity/hollow_warden/flame_1.png"),
            Wisplight.id("textures/entity/hollow_warden/flame_2.png"),
            Wisplight.id("textures/entity/hollow_warden/flame_3.png"),
    };

    public HollowWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new HollowWardenModel(context.bakeLayer(HollowWardenModel.LAYER)), 1.2F);
        addLayer(new FlameLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HollowWardenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(HollowWardenEntity entity, BlockPos pos) {
        return Math.max(9, super.getBlockLightLevel(entity, pos));
    }

    static final class FlameLayer extends RenderLayer<HollowWardenEntity, HollowWardenModel> {
        FlameLayer(RenderLayerParent<HollowWardenEntity, HollowWardenModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HollowWardenEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            int phase = Math.max(1, Math.min(3, entity.getPhase()));
            getParentModel().renderFlame(poseStack, buffer.getBuffer(RenderType.eyes(FLAMES[phase - 1])),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
