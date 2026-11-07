package com.futuretek.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.futuretek.logistics.ItemTransit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Disegna gli oggetti che viaggiano nei tubi logistici (SOLO client).
 * Ogni oggetto scorre da un centro di blocco al successivo a 6 blocchi al secondo.
 */
public final class TransitRenderer {
    private TransitRenderer() {
    }

    private static final double SPEED = 0.3; // blocchi per tick
    private static final int MAX_ACTIVE = 256;

    private record Transit(ItemStack item, List<Vec3> points, double start) {
    }

    private static final List<Transit> ACTIVE = new ArrayList<>();

    public static void add(ItemTransit.Payload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || payload.path().size() < 2 || ACTIVE.size() >= MAX_ACTIVE) {
            return;
        }
        List<Vec3> points = new ArrayList<>();
        for (BlockPos p : payload.path()) {
            points.add(Vec3.atCenterOf(p));
        }
        ACTIVE.add(new Transit(payload.item(), points, mc.level.getGameTime()));
    }

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        double now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Iterator<Transit> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Transit t = it.next();
            double travelled = (now - t.start()) * SPEED;
            int segment = (int) travelled;
            if (segment >= t.points().size() - 1 || travelled < 0) {
                it.remove();
                continue;
            }
            Vec3 a = t.points().get(segment);
            Vec3 b = t.points().get(segment + 1);
            Vec3 pos = a.lerp(b, travelled - segment);
            pose.pushPose();
            pose.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees((float) (now * 6 % 360)));
            pose.scale(0.35F, 0.35F, 0.35F);
            mc.getItemRenderer().renderStatic(t.item(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
