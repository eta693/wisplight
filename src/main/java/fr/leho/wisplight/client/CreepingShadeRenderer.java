package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.CreepingShadeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Silhouette humanoïde étirée, aux yeux blancs qui brillent dans le noir. */
public class CreepingShadeRenderer extends HumanoidMobRenderer<CreepingShadeEntity, HumanoidModel<CreepingShadeEntity>> {
    private static final ResourceLocation TEXTURE = Wisplight.id("textures/entity/creeping_shade.png");
    private static final RenderType EYES = RenderType.eyes(Wisplight.id("textures/entity/creeping_shade_eyes.png"));

    public CreepingShadeRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
        addLayer(new ShadeEyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CreepingShadeEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(CreepingShadeEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9F, 1.1F, 0.9F);
    }

    static final class ShadeEyes extends EyesLayer<CreepingShadeEntity, HumanoidModel<CreepingShadeEntity>> {
        ShadeEyes(RenderLayerParent<CreepingShadeEntity, HumanoidModel<CreepingShadeEntity>> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
