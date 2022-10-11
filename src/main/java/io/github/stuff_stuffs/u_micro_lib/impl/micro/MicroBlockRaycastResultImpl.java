package io.github.stuff_stuffs.u_micro_lib.impl.micro;

import com.mojang.datafixers.util.Either;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockHitResult;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockRaycastResult;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MicroBlockRaycastResultImpl implements MicroBlockRaycastResult {
    private final Type type;
    private final @Nullable BlockHitResult block;
    private final @Nullable MicroBlockHitResult micro;

    public MicroBlockRaycastResultImpl(final BlockHitResult block) {
        type = Type.BLOCK;
        this.block = block;
        micro = null;
    }

    public MicroBlockRaycastResultImpl(final MicroBlockHitResult micro) {
        type = Type.MICRO_BLOCK;
        block = null;
        this.micro = micro;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public BlockHitResult block() {
        if (type == Type.MICRO_BLOCK) {
            throw new RuntimeException("Cannot call block() on a MicroBlockRaycastResult of type MICRO_BLOCK");
        }
        return block;
    }

    @Override
    public MicroBlockHitResult micro() {
        if (type == Type.BLOCK) {
            throw new RuntimeException("Cannot call micro() on a MicroBlockRaycastResult of type BLOCK");
        }
        return micro;
    }

    @Override
    public Either<BlockHitResult, MicroBlockHitResult> either() {
        return switch (type) {
            case BLOCK -> Either.left(block);
            case MICRO_BLOCK -> Either.right(micro);
        };
    }
}
