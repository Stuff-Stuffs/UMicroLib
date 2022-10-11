package io.github.stuff_stuffs.u_micro_lib.internal.common.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockRaycastResult;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHitResultImpl;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockRaycastResultImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.UMicroLibBlocks;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class MicroWorldUtil {
    private MicroWorldUtil() {
    }

    public static Set<BlockPos> positions(final VoxelShape shape) {
        final LongSet encodedPositions = new LongOpenHashSet();
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            final int lowX = MathHelper.floor(minX);
            final int lowY = MathHelper.floor(minY);
            final int lowZ = MathHelper.floor(minZ);
            final int highX = MathHelper.ceil(maxX);
            final int highY = MathHelper.ceil(maxY);
            final int highZ = MathHelper.ceil(maxZ);
            for (int x = lowX; x <= highX; x++) {
                for (int y = lowY; y <= highY; y++) {
                    for (int z = lowZ; z <= highZ; z++) {
                        if (VoxelShapes.matchesAnywhere(VoxelShapes.fullCube().offset(x, y, z), shape, BooleanBiFunction.AND)) {
                            encodedPositions.add(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }
        });
        return Set.of(encodedPositions.longStream().mapToObj(BlockPos::fromLong).toArray(BlockPos[]::new));
    }

    public static BlockPos canonicalPosition(final VoxelShape shape) {
        if (shape.isEmpty()) {
            throw new IllegalArgumentException();
        }
        final long[] encoded = new long[]{BlockPos.asLong(30_000_000, 30_000_000, 30_000_000)};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            final int lowX = MathHelper.floor(minX);
            final int lowY = MathHelper.floor(minY);
            final int lowZ = MathHelper.floor(minZ);
            final int highX = MathHelper.ceil(maxX);
            final int highY = MathHelper.ceil(maxY);
            final int highZ = MathHelper.ceil(maxZ);
            for (int x = lowX; x <= highX; x++) {
                for (int y = lowY; y <= highY; y++) {
                    for (int z = lowZ; z <= highZ; z++) {
                        if (x <= BlockPos.unpackLongX(encoded[0]) && y <= BlockPos.unpackLongY(encoded[0]) && z <= BlockPos.unpackLongZ(encoded[0]) && VoxelShapes.matchesAnywhere(VoxelShapes.fullCube().offset(x, y, z), shape, BooleanBiFunction.AND)) {
                            encoded[0] = BlockPos.asLong(x, y, z);
                        }
                    }
                }
            }
        });
        return BlockPos.fromLong(encoded[0]);
    }

    public static boolean tryPlacePiece(final MicroBlockHandleImpl handle, final World world, final MicroBlock<?> piece) {
        final VoxelShape shape = piece.outlineShape();
        final Set<BlockPos> positions = positions(shape);
        for (final BlockPos position : positions) {
            final BlockEntity blockEntity = world.getBlockEntity(position);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity)) {
                final BlockState state = world.getBlockState(position);
                if (!state.isAir()) {
                    return false;
                }
            }
        }
        for (final BlockPos position : positions) {
            final BlockEntity blockEntity = world.getBlockEntity(position);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity)) {
                world.setBlockState(position, UMicroLibBlocks.MULTI_PART_BLOCK.getDefaultState());
            }
        }
        for (final BlockPos position : positions) {
            final BlockEntity blockEntity = world.getBlockEntity(position);
            if (blockEntity instanceof MicroBlockContainerBlockEntity microBlock) {
                final VoxelShape outlineShape = microBlock.getContainer().outlineShape();
                if (VoxelShapes.matchesAnywhere(outlineShape, piece.outlineShape(), BooleanBiFunction.AND)) {
                    return false;
                }
            }
        }
        for (final BlockPos position : positions) {
            final BlockEntity blockEntity = world.getBlockEntity(position);
            if (blockEntity instanceof MicroBlockContainerBlockEntity microBlockPiece) {
                microBlockPiece.getContainer().outlineShapeChange(handle.id(), piece, piece.outlineShape());
                microBlockPiece.getContainer().collisionShapeChange(handle.id(), piece, piece.collisionShape());
            }
        }
        ((MicroBlockWorldExtensions) world).updateCanonicalPos(handle.id(), piece.outlineShape());
        return true;
    }

    public static void breakPiece(final MicroBlockHandleImpl handle, final World world, final MicroBlock<?> piece) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) world;
        final long id = handle.id();
        final Set<BlockPos> positions = positions(piece.outlineShape());
        for (final BlockPos position : positions) {
            final BlockEntity blockEntity = world.getBlockEntity(position);
            if (blockEntity instanceof MicroBlockContainerBlockEntity microBlockPiece) {
                microBlockPiece.getContainer().removePiece(id);
            }
        }
        extensions.updateCanonicalPos(id, VoxelShapes.empty());
        piece.drop(world);
    }

    public static boolean tryResizeOutline(final MicroBlockHandleImpl handle, final World world, final VoxelShape newShape) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) world;
        final BlockPos blockPos = extensions.canonicalPos(handle);
        if (blockPos == null) {
            return false;
        }
        final BlockEntity entity = world.getBlockEntity(blockPos);
        if (entity instanceof MicroBlockContainerBlockEntity micro) {
            final MicroBlock<?> piece = micro.getContainer().byId(handle.id());
            if (piece == null) {
                return false;
            }
            final Set<BlockPos> newPositions = positions(newShape);
            for (final BlockPos position : newPositions) {
                final BlockEntity blockEntity = world.getBlockEntity(position);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                    final BlockState state = world.getBlockState(position);
                    if (!state.isAir()) {
                        return false;
                    }
                } else if (VoxelShapes.matchesAnywhere(microContainer.getContainer().outlineShapeWithout(handle.id()), newShape, BooleanBiFunction.AND)) {
                    return false;
                }
            }
            final Set<BlockPos> oldPositions = positions(piece.outlineShape());
            for (final BlockPos position : oldPositions) {
                final BlockEntity blockEntity = world.getBlockEntity(position);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                    throw new RuntimeException();
                }
                microContainer.getContainer().outlineShapeChange(handle.id(), piece, newShape);
            }
            for (final BlockPos position : newPositions) {
                BlockEntity blockEntity = world.getBlockEntity(position);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity)) {
                    final BlockState state = world.getBlockState(position);
                    if (!state.isAir()) {
                        throw new RuntimeException();
                    }
                    world.setBlockState(position, UMicroLibBlocks.MULTI_PART_BLOCK.getDefaultState());
                    blockEntity = world.getBlockEntity(position);
                }
                ((MicroBlockContainerBlockEntity) blockEntity).getContainer().outlineShapeChange(handle.id(), piece, newShape);
            }
            return true;
        } else {
            throw new RuntimeException();
        }
    }

    public static void resizeCollision(final MicroBlockHandleImpl handle, final World world, final VoxelShape newShape) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) world;
        final BlockPos blockPos = extensions.canonicalPos(handle);
        if (blockPos == null) {
            return;
        }
        final BlockEntity entity = world.getBlockEntity(blockPos);
        if (entity instanceof MicroBlockContainerBlockEntity micro) {
            final MicroBlock<?> piece = micro.getContainer().byId(handle.id());
            if (piece == null) {
                return;
            }
            final Set<BlockPos> oldPositions = positions(piece.outlineShape());
            for (final BlockPos position : oldPositions) {
                final BlockEntity blockEntity = world.getBlockEntity(position);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                    throw new RuntimeException();
                }
                microContainer.getContainer().collisionShapeChange(handle.id(), piece, newShape);
            }
        } else {
            throw new RuntimeException();
        }
    }

    public static Optional<MicroBlockRaycastResult> raycast(final Vec3d start, final Vec3d end, final MicroBlockWorld.MicroShapeType shapeType, final Predicate<FluidState> fluidHandler, final World world) {
        final int dx = (int) Math.signum(end.x - start.x);
        final double tDeltaX;
        if (dx != 0) {
            if ((end.x - start.x) != 0) {
                tDeltaX = Math.min(dx / (end.x - start.x), Double.MAX_VALUE);
            } else {
                tDeltaX = Double.MAX_VALUE;
            }
        } else {
            tDeltaX = Double.MAX_VALUE;
        }
        double tMaxX;
        if (dx < 0) {
            tMaxX = tDeltaX * MathHelper.fractionalPart(start.x);
        } else {
            tMaxX = tDeltaX * (1 - MathHelper.fractionalPart(start.x));
        }
        int x = MathHelper.floor(start.x);

        final int dy = (int) Math.signum(end.y - start.y);
        final double tDeltaY;
        if (dy != 0) {
            if ((end.y - start.y) != 0) {
                tDeltaY = Math.min(dy / (end.y - start.y), Double.MAX_VALUE);
            } else {
                tDeltaY = Double.MAX_VALUE;
            }
        } else {
            tDeltaY = Double.MAX_VALUE;
        }
        double tMaxY;
        if (dy < 0) {
            tMaxY = tDeltaY * MathHelper.fractionalPart(start.y);
        } else {
            tMaxY = tDeltaY * (1 - MathHelper.fractionalPart(start.y));
        }
        int y = MathHelper.floor(start.y);

        final int dz = (int) Math.signum(end.z - start.z);
        final double tDeltaZ;
        if (dz != 0) {
            if ((end.z - start.z) != 0) {
                tDeltaZ = Math.min(dz / (end.z - start.z), Double.MAX_VALUE);
            } else {
                tDeltaZ = Double.MAX_VALUE;
            }
        } else {
            tDeltaZ = Double.MAX_VALUE;
        }
        double tMaxZ;
        if (dz < 0) {
            tMaxZ = tDeltaZ * MathHelper.fractionalPart(start.z);
        } else {
            tMaxZ = tDeltaZ * (1 - MathHelper.fractionalPart(start.z));
        }
        int z = MathHelper.floor(start.z);
        final ShapeFunction shapeGetter = switch (shapeType) {
            case OUTLINE -> AbstractBlock.AbstractBlockState::getOutlineShape;
            case COLLISION -> AbstractBlock.AbstractBlockState::getCollisionShape;
        };
        final double lx = end.x - start.x;
        final double ly = end.y - start.y;
        final double lz = end.z - start.z;
        Vec3d bestPos = null;
        long best = Long.MIN_VALUE;
        Direction bestDirection = null;
        final LongSet visited = new LongOpenHashSet();
        final BlockPos.Mutable mutable = new BlockPos.Mutable();
        {
            final BlockState blockState = world.getBlockState(mutable.set(x, y, z));
            if (blockState.isOf(UMicroLibBlocks.MULTI_PART_BLOCK)) {
                final BlockEntity blockEntity = world.getBlockEntity(mutable);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                    throw new RuntimeException();
                }
                final MicroBlockContainerBlockEntity.Container container = microContainer.getContainer();
                final LongSet pieces = container.pieces();
                final LongIterator iterator = pieces.iterator();
                while (iterator.hasNext()) {
                    final long next = iterator.nextLong();
                    if (!visited.add(next)) {
                        continue;
                    }
                    final MicroBlock<?> microBlock = container.byId(next);
                    final VoxelShape shape = switch (shapeType) {
                        case OUTLINE -> microBlock.outlineShape();
                        case COLLISION -> microBlock.collisionShape();
                    };
                    final BlockHitResult result = shape.raycast(start, end, BlockPos.ORIGIN);
                    if (result != null && result.getType() == HitResult.Type.BLOCK) {
                        if (bestPos == null || bestPos.squaredDistanceTo(start) > result.getPos().squaredDistanceTo(start)) {
                            bestPos = result.getPos();
                            bestDirection = result.getSide();
                            best = next;
                        }
                    }
                }
            } else {
                final VoxelShape shape = shapeGetter.apply(blockState, world, mutable);
                final BlockHitResult raycast = shape.raycast(start, end, mutable);
                if (raycast != null && raycast.getType() == HitResult.Type.BLOCK) {
                    return Optional.of(new MicroBlockRaycastResultImpl(raycast.withBlockPos(mutable.toImmutable())));
                }
            }
        }
        while (
                !(tMaxX > 1 && tMaxY > 1 && tMaxZ > 1) && !(bestPos != null
                        && bestPos.squaredDistanceTo(start) < (lx * tMaxX) * (lx * tMaxX)
                        + (ly * tMaxY) * (ly * tMaxY)
                        + (lz * tMaxZ) * (lz * tMaxZ))
        ) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += dx;
                    tMaxX += tDeltaX;
                } else {
                    z += dz;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += dy;
                    tMaxY += tDeltaY;
                } else {
                    z += dz;
                    tMaxZ += tDeltaZ;
                }
            }
            final BlockState blockState = world.getBlockState(mutable.set(x, y, z));
            if (blockState.isOf(UMicroLibBlocks.MULTI_PART_BLOCK)) {
                final BlockEntity blockEntity = world.getBlockEntity(mutable);
                if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                    throw new RuntimeException();
                }
                final MicroBlockContainerBlockEntity.Container container = microContainer.getContainer();
                final LongSet pieces = container.pieces();
                final LongIterator iterator = pieces.iterator();
                while (iterator.hasNext()) {
                    final long next = iterator.nextLong();
                    if (!visited.add(next)) {
                        continue;
                    }
                    final MicroBlock<?> microBlock = container.byId(next);
                    final VoxelShape shape = switch (shapeType) {
                        case OUTLINE -> microBlock.outlineShape();
                        case COLLISION -> microBlock.collisionShape();
                    };
                    final BlockHitResult result = shape.raycast(start, end, BlockPos.ORIGIN);
                    if (result != null && result.getType() == HitResult.Type.BLOCK) {
                        if (bestPos == null || bestPos.squaredDistanceTo(start) > result.getPos().squaredDistanceTo(start)) {
                            bestPos = result.getPos();
                            bestDirection = result.getSide();
                            best = next;
                        }
                    }
                }
            } else {
                final VoxelShape shape = shapeGetter.apply(blockState, world, mutable);
                final BlockHitResult raycast = shape.raycast(start, end, mutable);
                if (raycast != null && raycast.getType() == HitResult.Type.BLOCK) {
                    if (bestPos == null || raycast.getPos().squaredDistanceTo(start) < bestPos.squaredDistanceTo(start)) {
                        return Optional.of(new MicroBlockRaycastResultImpl(raycast.withBlockPos(mutable.toImmutable())));
                    }
                }
            }
        }
        if (bestPos == null) {
            return Optional.empty();
        } else {
            return Optional.of(new MicroBlockRaycastResultImpl(new MicroBlockHitResultImpl(bestPos, false, bestDirection, new MicroBlockHandleImpl(world.getRegistryKey(), best))));
        }
    }

    private interface ShapeFunction {
        VoxelShape apply(BlockState state, BlockView world, BlockPos pos);
    }
}
