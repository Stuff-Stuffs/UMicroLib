package io.github.stuff_stuffs.u_micro_lib.api.common.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public interface MicroBlockHitResult {
    Vec3d hitPos();

    boolean inside();

    Direction hitSide();

    MicroBlockHandle handle();
}
