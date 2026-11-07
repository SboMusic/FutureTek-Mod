package com.futuretek.client;

import com.futuretek.FutureTek;
import com.futuretek.robot.RobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Disegna il robot con un modello "umanoide" e una texture originale (textures/entity/robot_N.png per lavoro) */
public class RobotRenderer extends HumanoidMobRenderer<RobotEntity, HumanoidModel<RobotEntity>> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "robot"), "main");
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[RobotEntity.MODES];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "textures/entity/robot_" + i + ".png");
        }
    }

    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.45F);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(RobotEntity robot) {
        return TEXTURES[Math.floorMod(robot.getMode(), TEXTURES.length)];
    }

    @Override
    protected void scale(RobotEntity robot, PoseStack pose, float partialTick) {
        pose.scale(0.9F, 0.9F, 0.9F);
    }
}
