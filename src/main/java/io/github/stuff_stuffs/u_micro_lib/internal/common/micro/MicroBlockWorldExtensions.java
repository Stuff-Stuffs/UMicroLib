package io.github.stuff_stuffs.u_micro_lib.internal.common.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.InitializableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import net.minecraft.util.shape.VoxelShape;

public interface MicroBlockWorldExtensions extends MicroBlockWorld {
    void updateCanonicalPos(long id, VoxelShape newShape);

    boolean tryLoadPiece(MicroBlockHandle handle, MicroBlock<?> piece);

    long getAndIncrementIdCounter();

    void breakPiece(long id, MicroBlock<?> piece);

    void addInitializableBlockEntity(InitializableBlockEntity be);

    void unloadPiece(long id, MicroBlock<?> piece);

    void microBlockBreakInfo(int entityId, long id, int stage);
}
