package io.github.stuff_stuffs.u_micro_lib.api.client.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface MicroBlockRenderer<ClientData> {
    boolean hasBakedModel();

    MicroBlockModel<ClientData> bakedModel(ClientData data);

    void renderDynamic(ClientData data, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay);
}
