package io.github.stuff_stuffs.u_micro_lib.api.common.micro.block;

import com.mojang.serialization.Lifecycle;
import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.function.Function;

public interface MicroBlockType<T extends MicroBlock<ClientData>, ClientData> {
    Registry<MicroBlockType<?, ?>> REGISTRY = FabricRegistryBuilder.from(new SimpleRegistry<>(RegistryKey.<MicroBlockType<?,?>>ofRegistry(UMicroLibCommon.id("micro_block_types")), Lifecycle.stable(), MicroBlockType::reference)).buildAndRegister();

    T create(Vec3d pos);

    RegistryEntry.Reference<MicroBlockType<?, ?>> reference();

    static <T extends MicroBlock<ClientData>, ClientData> MicroBlockType<T, ClientData> create(final Function<Vec3d, T> factory) {
        return new MicroBlockType<>() {
            private final RegistryEntry.Reference<MicroBlockType<?, ?>> reference = REGISTRY.createEntry(this);

            @Override
            public T create(final Vec3d pos) {
                return factory.apply(pos);
            }

            @Override
            public RegistryEntry.Reference<MicroBlockType<?, ?>> reference() {
                return reference;
            }
        };
    }
}
