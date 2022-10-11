package io.github.stuff_stuffs.u_micro_lib.api.common.micro.item;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface MicroBlockPlacementContext {
    World world();

    @Nullable Entity entity();

    Vec3d hitPos();

    Direction hitSide();

    ItemStack stack();

    Optional<MicroBlockHandle> tryPlace(MicroBlock<?> piece);
}
