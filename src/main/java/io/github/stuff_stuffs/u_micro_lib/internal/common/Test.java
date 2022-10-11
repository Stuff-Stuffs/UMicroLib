package io.github.stuff_stuffs.u_micro_lib.internal.common;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.item.MicroBlockPlacementContext;
import io.github.stuff_stuffs.u_micro_lib.api.common.util.MicroBlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class Test implements MicroBlock<Object> {
    private static final VoxelShape VOXEL_SHAPE = VoxelShapes.cuboid(0, 0, 0, 14 / 16.0, 14 / 16.0, 14 / 16.0);
    private final MicroBlockPos pos;

    public Test(final MicroBlockPos pos) {
        this.pos = pos;
    }

    @Override
    public MicroBlockType<?, Object> type() {
        return UMicroLibCommon.PIECE_TYPE;
    }

    @Override
    public Vec3d pos() {
        return pos.vec();
    }

    @Override
    public Object clientData() {
        return null;
    }

    @Override
    public BlockState blockStatePropertyProxy() {
        return Blocks.STONE.getDefaultState();
    }

    @Override
    public VoxelShape outlineShape() {
        final Vec3d vec = pos.vec();
        return VOXEL_SHAPE.offset(vec.x, vec.y, vec.z);
    }

    @Override
    public VoxelShape collisionShape() {
        final Vec3d vec = pos.vec();
        return VOXEL_SHAPE.offset(vec.x, vec.y, vec.z);
    }

    @Override
    public void drop(final World world) {

    }

    public static MicroBlockPos placementPosition(final MicroBlockPlacementContext context) {
        final Direction hitSide = context.hitSide();
        if (hitSide.getDirection() == Direction.AxisDirection.NEGATIVE) {
            return new MicroBlockPos(context.hitPos().subtract(hitSide.getOffsetX() * 14 / 16.0, hitSide.getOffsetY() * 14 / 16.0, hitSide.getOffsetZ() * 14 / 16.0));
        }
        return new MicroBlockPos(context.hitPos());
    }
}
