package io.github.stuff_stuffs.u_micro_lib.internal.client.render.model;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.Vec3d;

public record MicroBlockContainerRenderAttachment(Long2ObjectMap<Entry<?>> entriesById) {
    public record Entry<T>(MicroBlockType<?, T> type, Vec3d pos, T data) {
    }
}
