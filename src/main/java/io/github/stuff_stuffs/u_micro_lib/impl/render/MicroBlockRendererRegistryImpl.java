package io.github.stuff_stuffs.u_micro_lib.impl.render;

import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockRenderer;
import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockRendererRegistry;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

public class MicroBlockRendererRegistryImpl implements MicroBlockRendererRegistry {
    public static final MicroBlockRendererRegistryImpl INSTANCE = new MicroBlockRendererRegistryImpl();
    private final Map<MicroBlockType<?, ?>, MicroBlockRenderer<?>> renderers = new Object2ReferenceOpenHashMap<>();
    private boolean verified = false;

    @Override
    public <ClientData> void register(final MicroBlockType<?, ClientData> type, final MicroBlockRenderer<ClientData> renderer) {
        if (renderers.put(type, renderer) != null) {
            throw new RuntimeException("Duplicate micro block renderers!");
        }
    }

    public <ClientData> MicroBlockRenderer<ClientData> renderer(final MicroBlockType<?, ClientData> type) {
        if (!verified) {
            for (final MicroBlockType<?, ?> pieceType : MicroBlockType.REGISTRY) {
                if (renderers.get(pieceType) == null) {
                    throw new RuntimeException("Micro block type " + MicroBlockType.REGISTRY.getId(pieceType) + " is missing a renderer!");
                }
            }
            verified = true;
        }
        return (MicroBlockRenderer<ClientData>) renderers.get(type);
    }
}
