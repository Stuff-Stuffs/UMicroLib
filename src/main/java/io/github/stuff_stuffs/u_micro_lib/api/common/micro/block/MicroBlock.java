package io.github.stuff_stuffs.u_micro_lib.api.common.micro.block;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public interface MicroBlock<ClientData> {
    MicroBlockType<?, ClientData> type();

    Vec3d pos();

    ClientData clientData();

    default void writeToNbt(final NbtCompound nbt) {
    }

    default void readFromNbt(final NbtCompound nbt) {
    }

    default void tick(final MicroBlockContext context) {
    }

    default void onBreakStart(final MicroBlockContext context, final Direction direction, final PlayerEntity entity) {
    }

    BlockState blockStatePropertyProxy();

    default float calcBlockBreakingDelta(final MicroBlockContext context, final PlayerEntity player) {
        return blockStatePropertyProxy().calcBlockBreakingDelta(player, context.world(), new BlockPos(pos()));
    }

    default BlockSoundGroup blockSoundGroup() {
        return blockStatePropertyProxy().getBlock().getSoundGroup(blockStatePropertyProxy());
    }

    default void onBreak(final MicroBlockContext context, final PlayerEntity entity) {
        final World world = context.world();
        if (world instanceof ClientWorld clientWorld) {
            clientWorld.playSound(pos().x, pos().y, pos().z, blockSoundGroup().getBreakSound(), SoundCategory.BLOCKS, (blockSoundGroup().getVolume() + 1.0F) / 2.0F, blockSoundGroup().getPitch() * 0.8F, false);
            final VoxelShape voxelShape = outlineShape();
            final double d = 0.25;
            voxelShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        final double dx = Math.min(1.0, maxX - minX);
                        final double e = Math.min(1.0, maxY - minY);
                        final double f = Math.min(1.0, maxZ - minZ);
                        final int i = Math.max(2, MathHelper.ceil(dx / d));
                        final int j = Math.max(2, MathHelper.ceil(e / d));
                        final int k = Math.max(2, MathHelper.ceil(f / d));

                        for (int l = 0; l < i; ++l) {
                            for (int m = 0; m < j; ++m) {
                                for (int n = 0; n < k; ++n) {
                                    final double g = ((double) l + 0.5) / (double) i;
                                    final double h = ((double) m + 0.5) / (double) j;
                                    final double o = ((double) n + 0.5) / (double) k;
                                    final double p = g * dx + minX;
                                    final double q = h * e + minY;
                                    final double r = o * f + minZ;
                                    MinecraftClient.getInstance().particleManager.addParticle(new BlockDustParticle(clientWorld, p, q,  r, g - 0.5, h - 0.5, o - 0.5, blockStatePropertyProxy(), new BlockPos(pos().x + p, pos().y + q, pos().z + r)));
                                }
                            }
                        }
                    }
            );
        }
    }

    default VoxelShape outlineShape() {
        return VoxelShapes.fullCube();
    }

    default VoxelShape collisionShape() {
        return VoxelShapes.fullCube();
    }

    void drop(World world);
}
