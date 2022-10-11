package io.github.stuff_stuffs.u_micro_lib.api.client.render;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import io.github.stuff_stuffs.u_micro_lib.impl.render.MicroBlockRendererRegistryImpl;

public interface MicroBlockRendererRegistry {
    <ClientData> void register(MicroBlockType<?, ClientData> type, MicroBlockRenderer<ClientData> renderer);

    static MicroBlockRendererRegistry get() {
        return MicroBlockRendererRegistryImpl.INSTANCE;
    }
}
