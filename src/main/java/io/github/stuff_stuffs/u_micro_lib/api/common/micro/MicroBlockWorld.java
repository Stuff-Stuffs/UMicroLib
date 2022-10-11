package io.github.stuff_stuffs.u_micro_lib.api.common.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockContext;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.item.MicroBlockPlacementContext;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public interface MicroBlockWorld {
    @Nullable BlockPos canonicalPos(MicroBlockHandle handle);

    MicroBlockContext createContext(MicroBlockHandle handle);

    MicroBlockPlacementContext createPlacementContext(@Nullable Entity entity, Vec3d hitPos, Direction hitSide, ItemStack stack);

    Optional<MicroBlockRaycastResult> microRaycast(Vec3d start, Vec3d end, MicroShapeType shapeType, Predicate<FluidState> fluidHandler);

    enum MicroShapeType {
        OUTLINE,
        COLLISION
    }
}
