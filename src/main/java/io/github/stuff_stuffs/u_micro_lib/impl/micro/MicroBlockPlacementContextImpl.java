package io.github.stuff_stuffs.u_micro_lib.impl.micro;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.item.MicroBlockPlacementContext;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MicroBlockPlacementContextImpl implements MicroBlockPlacementContext {
    private final World world;
    private final @Nullable Entity entity;
    private final Vec3d hitPos;
    private final Direction hitSide;
    private final ItemStack stack;

    public MicroBlockPlacementContextImpl(final World world, @Nullable final Entity entity, final Vec3d hitPos, final Direction hitSide, final ItemStack stack) {
        this.world = world;
        this.entity = entity;
        this.hitPos = hitPos;
        this.hitSide = hitSide;
        this.stack = stack;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public @Nullable Entity entity() {
        return entity;
    }

    @Override
    public Vec3d hitPos() {
        return hitPos;
    }

    @Override
    public Direction hitSide() {
        return hitSide;
    }

    @Override
    public ItemStack stack() {
        return stack;
    }

    @Override
    public Optional<MicroBlockHandle> tryPlace(final MicroBlock<?> piece) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) world;
        final long id = extensions.getAndIncrementIdCounter();
        final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(world.getRegistryKey(), id);
        if (extensions.tryLoadPiece(handle, piece)) {
            return Optional.of(handle);
        }
        return Optional.empty();
    }
}
