package io.github.stuff_stuffs.u_micro_lib.internal.common.block;

import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricMaterialBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class MicroBlockContainerBlock extends BlockWithEntity {
    public static final Material MATERIAL = new FabricMaterialBuilder(MapColor.CLEAR).blocksPistons().build();

    public MicroBlockContainerBlock() {
        super(FabricBlockSettings.of(MATERIAL).dropsNothing().strength(-1, Float.POSITIVE_INFINITY).dynamicBounds().nonOpaque());
    }

    @Override
    public VoxelShape getOutlineShape(final BlockState state, final BlockView world, final BlockPos pos, final ShapeContext context) {
        final BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MicroBlockContainerBlockEntity microBlock) {
            return microBlock.getContainer().outlineShape().offset(-pos.getX(), -pos.getY(), -pos.getZ());
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockView world, final BlockPos pos, final ShapeContext context) {
        final BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MicroBlockContainerBlockEntity microBlock) {
            return microBlock.getContainer().collisionShape().offset(-pos.getX(), -pos.getY(), -pos.getZ());
        }
        return VoxelShapes.fullCube();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new MicroBlockContainerBlockEntity(pos, state);
    }
}
