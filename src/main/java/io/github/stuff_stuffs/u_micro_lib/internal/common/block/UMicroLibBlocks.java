package io.github.stuff_stuffs.u_micro_lib.internal.common.block;

import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.UMicroLibBlockEntities;
import net.minecraft.util.registry.Registry;

public final class UMicroLibBlocks {
    public static final MicroBlockContainerBlock MULTI_PART_BLOCK = new MicroBlockContainerBlock();

    public static void init() {
        Registry.register(Registry.BLOCK, UMicroLibCommon.id("multi_part"), MULTI_PART_BLOCK);
        UMicroLibBlockEntities.init();
    }

    private UMicroLibBlocks() {
    }
}
