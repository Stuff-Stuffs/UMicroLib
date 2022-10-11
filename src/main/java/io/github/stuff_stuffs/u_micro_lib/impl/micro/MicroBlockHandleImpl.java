package io.github.stuff_stuffs.u_micro_lib.impl.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public record MicroBlockHandleImpl(RegistryKey<World> worldKey, long id) implements MicroBlockHandle {
}
