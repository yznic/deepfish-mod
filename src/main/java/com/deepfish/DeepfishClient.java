package com.deepfish;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
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

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(DeepfishClient::render);
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
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
        if (data == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (!p.getName().getString().equals("deepdeep")) continue;
            Vec3d pos = p.getLerpedPos(ctx.tickDelta());
            float yaw = p.prevBodyYaw + (p.bodyYaw - p.prevBodyYaw) * ctx.tickDelta();
            MatrixStack ms = ctx.matrixStack();
            Vec3d cam = ctx.camera().getPos();
            ms.push();
            ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - yaw));
            ms.scale(1f / 16f, 1f / 16f, 1f / 16f);
            Matrix4f mat = ms.peek().getPositionMatrix();
            Matrix3f nm = new Matrix3f(ms.peek().getNormalMatrix());
            VertexConsumer vc = ctx.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
            for (QuadData.Quad q : data.quads) {
                Vector3f n = new Vector3f(q.n[0], q.n[1], q.n[2]).mul(nm).normalize();
                for (int i = 0; i < 4; i++) {
                    Vector3f pos = new Vector3f(q.p[i][0], q.p[i][1], q.p[i][2]);
                    mat.transformPosition(pos);
                    vc.vertex(pos.x(), pos.y(), pos.z(), 1f, 1f, 1f, 1f,
                            q.uv[i][0], q.uv[i][1], OverlayTexture.DEFAULT_UV, 0xF000F0,
                            n.x(), n.y(), n.z());
                }
            }
            ms.pop();
        }
    }
}
