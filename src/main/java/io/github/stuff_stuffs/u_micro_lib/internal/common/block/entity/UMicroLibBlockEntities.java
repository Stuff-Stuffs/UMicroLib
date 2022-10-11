package io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity;

import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.UMicroLibBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public final class UMicroLibBlockEntities {
    public static final BlockEntityType<MicroBlockContainerBlockEntity> MULTI_PART_BLOCK_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(MicroBlockContainerBlockEntity::new, UMicroLibBlocks.MULTI_PART_BLOCK).build();

    public static void init() {
        Registry.register(Registry.BLOCK_ENTITY_TYPE, UMicroLibCommon.id("multi_part"), MULTI_PART_BLOCK_ENTITY_TYPE);
    }

    private UMicroLibBlockEntities() {
    }
}
