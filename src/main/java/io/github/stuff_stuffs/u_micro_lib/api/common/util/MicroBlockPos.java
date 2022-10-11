package io.github.stuff_stuffs.u_micro_lib.api.common.util;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MicroBlockPos {
    public static final int INCREMENTS_PRE_BLOCK = 32;
    private final BlockPos pos;
    private final int xOff;
    private final int yOff;
    private final int zOff;

    public MicroBlockPos(final NbtLongArray arr) {
        this(BlockPos.fromLong(arr.get(0).longValue()), BlockPos.unpackLongX(arr.get(1).longValue()), BlockPos.unpackLongY(arr.get(1).longValue()), BlockPos.unpackLongZ(arr.get(1).longValue()));
    }

    public MicroBlockPos(final Vec3d vec) {
        this(new BlockPos(vec), (int) ((vec.x - MathHelper.floor(vec.x)) * 16.0), (int) ((vec.y - MathHelper.floor(vec.y)) * 16.0), (int) ((vec.z - MathHelper.floor(vec.z)) * 16.0));
    }

    public MicroBlockPos(BlockPos pos, int xOff, int yOff, int zOff) {
        if (xOff < 0) {
            pos = pos.offset(Direction.Axis.X, xOff / INCREMENTS_PRE_BLOCK - 1);
            xOff = xOff % INCREMENTS_PRE_BLOCK + INCREMENTS_PRE_BLOCK;
        } else if (INCREMENTS_PRE_BLOCK <= xOff) {
            pos = pos.offset(Direction.Axis.X, xOff / INCREMENTS_PRE_BLOCK);
            xOff = xOff % INCREMENTS_PRE_BLOCK;
        }

        if (yOff < 0) {
            pos = pos.offset(Direction.Axis.Y, yOff / INCREMENTS_PRE_BLOCK - 1);
            yOff = yOff % INCREMENTS_PRE_BLOCK + INCREMENTS_PRE_BLOCK;
        } else if (INCREMENTS_PRE_BLOCK <= yOff) {
            pos = pos.offset(Direction.Axis.Y, yOff / INCREMENTS_PRE_BLOCK);
            yOff = yOff % INCREMENTS_PRE_BLOCK;
        }

        if (zOff < 0) {
            pos = pos.offset(Direction.Axis.Z, zOff / INCREMENTS_PRE_BLOCK - 1);
            zOff = zOff % INCREMENTS_PRE_BLOCK + INCREMENTS_PRE_BLOCK;
        } else if (INCREMENTS_PRE_BLOCK <= zOff) {
            pos = pos.offset(Direction.Axis.Z, zOff / INCREMENTS_PRE_BLOCK);
            zOff = zOff % INCREMENTS_PRE_BLOCK;
        }
        this.pos = pos;
        this.xOff = xOff;
        this.yOff = yOff;
        this.zOff = zOff;
    }

    public MicroBlockPos add(final MicroBlockPos pos) {
        return new MicroBlockPos(new BlockPos(this.pos.add(pos.pos)), xOff + pos.xOff, yOff + pos.yOff, zOff + pos.zOff);
    }

    public MicroBlockPos withBlockPos(final BlockPos pos) {
        return new MicroBlockPos(pos.toImmutable(), xOff, yOff, zOff);
    }

    public MicroBlockPos withMicroPos(final int x, final int y, final int z) {
        return new MicroBlockPos(pos, x, y, z);
    }

    public MicroBlockPos addMicro(final int x, final int y, final int z) {
        return new MicroBlockPos(pos, x + xOff, y + yOff, z + zOff);
    }

    public BlockPos pos() {
        return pos;
    }

    public int microX() {
        return xOff;
    }

    public int microY() {
        return yOff;
    }

    public int microZ() {
        return zOff;
    }

    public Vec3d vec() {
        return new Vec3d(pos.getX() + xOff / 16.0, pos.getY() + yOff / 16.0, pos.getZ() + zOff / 16.0);
    }

    public NbtElement toNbt() {
        return new NbtLongArray(new long[]{pos.asLong(), BlockPos.asLong(xOff, yOff, zOff)});
    }
}
