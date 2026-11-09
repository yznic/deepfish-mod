package com.deepfish;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;

public class DeepfishClient implements ClientModInitializer {
    public static final Identifier TEX = new Identifier("deepfish", "textures/model.png");
    public static final Identifier QUADS = new Identifier("deepfish", "quads.json");
    private static QuadData data;
    private static boolean triedLoad = false;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(DeepfishClient::render);
    }

    private static void ensureLoaded(MinecraftClient mc) {
        if (data != null || triedLoad) return;
        triedLoad = true;
        try {
            try (InputStream in = mc.getResourceManager().getResource(QUADS).orElseThrow().getInputStream()) {
                data = new Gson().fromJson(new String(in.readAllBytes()), QuadData.class);
            }
            try (InputStream in = mc.getResourceManager().getResource(TEX).orElseThrow().getInputStream()) {
                NativeImage img = NativeImage.read(in);
                mc.getTextureManager().registerTexture(TEX, new NativeImageBackedTexture(img));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ensureLoaded(mc);
        if (data == null || mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (!p.getName().getString().equals("deepdeep")) continue;
            Vec3d pos = p.getLerpedPos(ctx.tickDelta());
            float yaw = p.prevBodyYaw + (p.bodyYaw - p.prevBodyYaw) * ctx.tickDelta();
            MatrixStack ms = ctx.matrixStack();
            Vec3d cam = ctx.camera().getPos();
            ms.push();
            ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            ms.translate(0f, -2.855f / 16f, 0f);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - yaw));
            ms.scale(1f / 16f, 1f / 16f, 1f / 16f);
            Matrix4f mat = ms.peek().getPositionMatrix();
            Matrix3f nm = new Matrix3f(ms.peek().getNormalMatrix());
            VertexConsumerProvider consumers = ctx.consumers();
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
            for (QuadData.Quad q : data.quads) {
                Vector3f n = new Vector3f(q.n[0], q.n[1], q.n[2]).mul(nm).normalize();
                for (int i = 0; i < 4; i++) {
                    Vector3f vp = new Vector3f(q.p[i][0], q.p[i][1], q.p[i][2]);
                    mat.transformPosition(vp);
                    vc.vertex(vp.x(), vp.y(), vp.z(), 1f, 1f, 1f, 1f,
                            q.uv[i][0], q.uv[i][1], OverlayTexture.DEFAULT_UV, 0xF000F0,
                            n.x(), n.y(), n.z());
                }
            }
            ItemRenderer ir = mc.getItemRenderer();
            ItemStack held = p.getMainHandStack();
            if (!held.isEmpty()) {
                ms.push();
                ms.translate(5.2f, 14.5f, -1.5f);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35f - p.handSwingProgress * 60f));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20f));
                ms.scale(12f, 12f, 12f);
                BakedModel model = ir.getModel(held, mc.world, p, p.getId());
                ir.renderItem(held, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, ms, consumers,
                        0xF000F0, OverlayTexture.DEFAULT_UV, model);
                ms.pop();
            }
            ItemStack off = p.getOffHandStack();
            if (!off.isEmpty()) {
                ms.push();
                ms.translate(-4.8f, 15.0f, 0.4f);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-p.handSwingProgress * 60f));
                ms.scale(12f, 12f, 12f);
                BakedModel model = ir.getModel(off, mc.world, p, p.getId());
                ir.renderItem(off, ModelTransformationMode.THIRD_PERSON_LEFT_HAND, true, ms, consumers,
                        0xF000F0, OverlayTexture.DEFAULT_UV, model);
                ms.pop();
            }
            ms.pop();
            if (consumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw();
            }
        }
    }
}
