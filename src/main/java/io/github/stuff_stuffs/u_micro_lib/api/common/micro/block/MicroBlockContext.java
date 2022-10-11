package io.github.stuff_stuffs.u_micro_lib.api.common.micro.block;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public interface MicroBlockContext {
    MicroBlockHandle handle();

    World world();

    boolean tryResizeOutline(VoxelShape newShape);

    void resizeCollision(VoxelShape newShape);

    void updateModel();

    void breakSelf();
}
