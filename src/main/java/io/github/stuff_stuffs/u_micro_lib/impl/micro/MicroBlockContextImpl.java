package io.github.stuff_stuffs.u_micro_lib.impl.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockContext;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroWorldUtil;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class MicroBlockContextImpl implements MicroBlockContext {
    private final MicroBlockHandleImpl handle;
    private final MicroBlock<?> piece;
    private final World world;

    public MicroBlockContextImpl(final MicroBlockHandleImpl handle, final MicroBlock<?> piece, final World world) {
        this.handle = handle;
        this.piece = piece;
        this.world = world;
    }

    @Override
    public MicroBlockHandle handle() {
        return handle;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public boolean tryResizeOutline(final VoxelShape newShape) {
        if (newShape.isEmpty()) {
            return false;
        }
        return MicroWorldUtil.tryResizeOutline(handle, world, newShape);
    }

    @Override
    public void resizeCollision(final VoxelShape newShape) {
        MicroWorldUtil.resizeCollision(handle, world, newShape);
    }

    @Override
    public void updateModel() {
        UMicroLibCommon.MODEL_UPDATER.accept(world, ((MicroBlockWorldExtensions) world).canonicalPos(handle));
    }

    @Override
    public void breakSelf() {
        MicroWorldUtil.breakPiece(handle, world, piece);
    }
}
