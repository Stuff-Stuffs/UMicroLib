package io.github.stuff_stuffs.u_micro_lib.internal.common.micro;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface ServerPlayerInteractionManagerExtensions {
    void startBreaking(long id, Direction direction, BlockPos pos);
}
