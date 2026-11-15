package fr.leho.wisplight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.entity.HollowWardenEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Le Veilleur Éteint : socle, quatre barreaux d'angle, toit à deux étages, anneau, deux chaînes
 * qui balancent et une flamme intérieure rendue à part (lumineuse).
 */
public class HollowWardenModel extends EntityModel<HollowWardenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wisplight.id("hollow_warden"), "main");

    private final ModelPart body;
    private final ModelPart flame;
    private final ModelPart leftChain;
    private final ModelPart rightChain;

    public HollowWardenModel(ModelPart root) {
        this.body = root.getChild("body");
        this.flame = root.getChild("flame");
        this.leftChain = body.getChild("left_chain");
        this.rightChain = body.getChild("right_chain");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-10.0F, -4.0F, -10.0F, 20.0F, 4.0F, 20.0F)
                        .texOffs(0, 24).addBox(-10.0F, -36.0F, -10.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(0, 24).addBox(7.0F, -36.0F, -10.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(0, 24).addBox(-10.0F, -36.0F, 7.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(0, 24).addBox(7.0F, -36.0F, 7.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(0, 64).addBox(-12.0F, -40.0F, -12.0F, 24.0F, 4.0F, 24.0F)
                        .texOffs(0, 92).addBox(-7.0F, -44.0F, -7.0F, 14.0F, 4.0F, 14.0F)
                        .texOffs(60, 92).addBox(-2.0F, -50.0F, -1.0F, 4.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        body.addOrReplaceChild("left_chain", CubeListBuilder.create()
                        .texOffs(60, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F),
                PartPose.offset(12.0F, -38.0F, 0.0F));
        body.addOrReplaceChild("right_chain", CubeListBuilder.create()
                        .texOffs(60, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F),
                PartPose.offset(-12.0F, -38.0F, 0.0F));
        root.addOrReplaceChild("flame", CubeListBuilder.create()
                        .texOffs(16, 24).addBox(-5.0F, -18.0F, -5.0F, 10.0F, 18.0F, 10.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HollowWardenEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 0.5F) * 0.5F * limbSwingAmount;
        leftChain.zRot = -0.15F + swing + Mth.sin(ageInTicks * 0.08F) * 0.05F;
        rightChain.zRot = 0.15F - swing - Mth.sin(ageInTicks * 0.08F) * 0.05F;
        leftChain.xRot = swing;
        rightChain.xRot = -swing;
        float charge = Math.min(1.0F, entity.getBurstCharge() / (float) HollowWardenEntity.BURST_WINDUP);
        float flicker = 1.0F + Mth.sin(ageInTicks * 0.35F) * 0.06F + charge * 0.25F;
        flame.xScale = flicker;
        flame.zScale = flicker;
        flame.yScale = flicker + Mth.sin(ageInTicks * 0.21F) * 0.05F;
        body.y = 24.0F + Mth.sin(ageInTicks * 0.06F) * 0.4F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderFlame(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        flame.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
