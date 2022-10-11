package io.github.stuff_stuffs.u_micro_lib.api.common.micro;

import com.mojang.datafixers.util.Either;
import net.minecraft.util.hit.BlockHitResult;

public interface MicroBlockRaycastResult {
    Type type();

    BlockHitResult block();

    MicroBlockHitResult micro();

    Either<BlockHitResult, MicroBlockHitResult> either();

    enum Type {
        BLOCK,
        MICRO_BLOCK
    }
}
