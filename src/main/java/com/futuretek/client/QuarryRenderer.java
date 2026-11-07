package com.futuretek.client;

import org.joml.Matrix4f;

import com.futuretek.FutureTek;
import com.futuretek.blockentity.QuarryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Disegna il carrello dell'escavatore: due travi sottili che scorrono sull'anello alto del telaio,
 * l'asta che scende fino al blocco che sta scavando e la trivella in fondo.
 * Il carrello si muove in modo fluido verso il blocco che il server ha scavato per ultimo.
 */
public class QuarryRenderer implements BlockEntityRenderer<QuarryBlockEntity> {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "block/quarry_frame");
    private static final ResourceLocation DRILL = ResourceLocation.fromNamespaceAndPath(FutureTek.MODID, "block/quarry_drill");
    private static final float A = 6 / 16F; // le barre sono spesse 4 pixel, come il telaio
    private static final float B = 10 / 16F;

    public QuarryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuarryBlockEntity quarry, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        BlockPos target = quarry.getTarget();
        if (!quarry.hasGantry() || target == null || quarry.getLevel() == null) {
            return;
        }
        // movimento fluido verso il bersaglio
        if (Float.isNaN(quarry.renderX)) {
            quarry.renderX = target.getX();
            quarry.renderY = target.getY();
            quarry.renderZ = target.getZ();
        }
        quarry.renderX += (target.getX() - quarry.renderX) * 0.15F;
        quarry.renderY += (target.getY() - quarry.renderY) * 0.15F;
        quarry.renderZ += (target.getZ() - quarry.renderZ) * 0.15F;

        TextureAtlasSprite frame = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FRAME);
        TextureAtlasSprite drill = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(DRILL);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        BlockPos origin = quarry.getBlockPos();
        pose.pushPose();
        pose.translate(-origin.getX(), -origin.getY(), -origin.getZ()); // da qui in poi coordinate del mondo
        float top = quarry.getTopY() + QuarryBlockEntity.FRAME_HEIGHT;
        float x = quarry.renderX;
        float z = quarry.renderZ;
        // trave lungo X (alla z del carrello) e trave lungo Z (alla x del carrello), a blocchi
        for (int bx = quarry.getMinX() - 1; bx <= quarry.getMaxX() + 1; bx++) {
            box(pose, vc, frame, light(quarry, bx, top, z), bx, top + A, z + A, bx + 1, top + B, z + B);
        }
        for (int bz = quarry.getMinZ() - 1; bz <= quarry.getMaxZ() + 1; bz++) {
            box(pose, vc, frame, light(quarry, x, top, bz), x + A, top + A, bz, x + B, top + B, bz + 1);
        }
        // asta verticale fino alla trivella
        float bottom = quarry.renderY + 1;
        for (float y = top; y > bottom; y -= 1) {
            float y0 = Math.max(bottom, y - 1);
            box(pose, vc, frame, light(quarry, x, y0, z), x + 7 / 16F, y0, z + 7 / 16F, x + 9 / 16F, y + A, z + 9 / 16F);
        }
        // trivella: un blocchetto con la punta sopra il blocco scavato
        box(pose, vc, drill, light(quarry, x, bottom, z), x + 4 / 16F, bottom, z + 4 / 16F, x + 12 / 16F, bottom + 0.75F, z + 12 / 16F);
        pose.popPose();
    }

    private static int light(QuarryBlockEntity quarry, float x, float y, float z) {
        return LevelRenderer.getLightColor(quarry.getLevel(), BlockPos.containing(x + 0.5, y + 0.5, z + 0.5));
    }

    /** Un parallelepipedo con la texture "sprite" su tutte e 6 le facce */
    private static void box(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, int light,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        Matrix4f m = pose.last().pose();
        PoseStack.Pose p = pose.last();
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        // giu' / su
        quad(vc, m, p, s, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
        quad(vc, m, p, s, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1);
        // nord / sud
        quad(vc, m, p, s, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1);
        quad(vc, m, p, s, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1);
        // ovest / est
        quad(vc, m, p, s, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1);
        quad(vc, m, p, s, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, TextureAtlasSprite s, int light,
            float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float u0, float v0, float u1, float v1) {
        vertex(vc, m, p, light, nx, ny, nz, ax, ay, az, u0, v1);
        vertex(vc, m, p, light, nx, ny, nz, bx, by, bz, u1, v1);
        vertex(vc, m, p, light, nx, ny, nz, cx, cy, cz, u1, v0);
        vertex(vc, m, p, light, nx, ny, nz, dx, dy, dz, u0, v0);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, int light, float nx, float ny, float nz,
            float x, float y, float z, float u, float v) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(QuarryBlockEntity quarry) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRender(QuarryBlockEntity quarry, Vec3 camera) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(QuarryBlockEntity quarry) {
        return new AABB(quarry.getMinX() - 1, quarry.getLevel() == null ? -64 : quarry.getLevel().getMinBuildHeight(),
                quarry.getMinZ() - 1, quarry.getMaxX() + 2, quarry.getTopY() + QuarryBlockEntity.FRAME_HEIGHT + 1,
                quarry.getMaxZ() + 2);
    }
}
