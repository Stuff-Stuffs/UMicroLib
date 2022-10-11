package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.InitializableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockRaycastResult;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockContext;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockHandle;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.item.MicroBlockPlacementContext;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockContextImpl;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockPlacementContextImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroWorldUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(World.class)
public abstract class MixinWorld implements MicroBlockWorldExtensions, WorldAccess {
    @Shadow
    public abstract RegistryKey<World> getRegistryKey();

    @Shadow
    private boolean iteratingTickingBlockEntities;

    @Unique
    private final Long2ObjectMap<BlockPos> canonicalPositions = new Long2ObjectOpenHashMap<>();
    @Unique
    private long idCounter = Long.MIN_VALUE + 1;
    @Unique
    private final List<InitializableBlockEntity> initializableBlockEntities = new ArrayList<>();
    @Unique
    private final List<InitializableBlockEntity> pendingInitializableBlockEntities = new ArrayList<>();

    @Override
    public BlockPos canonicalPos(final MicroBlockHandle handle) {
        final MicroBlockHandleImpl handleImpl = (MicroBlockHandleImpl) handle;
        if (!handleImpl.worldKey().equals((((World) (Object) this).getRegistryKey()))) {
            throw new IllegalArgumentException();
        }
        return canonicalPositions.containsKey(handleImpl.id()) ? canonicalPositions.get(handleImpl.id()) : null;
    }

    @Override
    public MicroBlockContext createContext(final MicroBlockHandle handle) {
        final MicroBlockHandleImpl handleImpl = (MicroBlockHandleImpl) handle;
        if (!handleImpl.worldKey().equals(((World) (Object) this).getRegistryKey())) {
            throw new IllegalArgumentException();
        }
        return new MicroBlockContextImpl(handleImpl, ((MicroBlockContainerBlockEntity) getBlockEntity(canonicalPositions.get(handleImpl.id()))).getContainer().byId(handleImpl.id()), ((World) (Object) this));
    }

    @Override
    public void updateCanonicalPos(final long id, final VoxelShape newShape) {
        if (newShape.isEmpty()) {
            canonicalPositions.remove(id);
        } else {
            final BlockPos canonicalPosition = MicroWorldUtil.canonicalPosition(newShape);
            canonicalPositions.put(id, canonicalPosition);
        }
    }

    @Override
    public boolean tryLoadPiece(final MicroBlockHandle handle, final MicroBlock<?> piece) {
        final BlockPos canonicalPosition = MicroWorldUtil.canonicalPosition(piece.outlineShape());
        final long id = ((MicroBlockHandleImpl) handle).id();
        if (canonicalPositions.containsKey(id)) {
            return canonicalPosition.equals(canonicalPositions.get(id));
        }
        if (MicroWorldUtil.tryPlacePiece((MicroBlockHandleImpl) handle, ((World) (Object) this), piece)) {
            canonicalPositions.put(id, canonicalPosition);
            return true;
        }
        return false;
    }

    @Override
    public MicroBlockPlacementContext createPlacementContext(@Nullable final Entity entity, final Vec3d hitPos, final Direction hitSide, final ItemStack stack) {
        return new MicroBlockPlacementContextImpl(((World) (Object) this), entity, hitPos, hitSide, stack);
    }

    @Override
    public long getAndIncrementIdCounter() {
        return idCounter++;
    }

    @Override
    public void breakPiece(final long id, final MicroBlock<?> piece) {
        MicroWorldUtil.breakPiece(new MicroBlockHandleImpl(getRegistryKey(), id), (World) (Object) this, piece);
    }

    @Override
    public void addInitializableBlockEntity(final InitializableBlockEntity be) {
        if (iteratingTickingBlockEntities) {
            pendingInitializableBlockEntities.add(be);
        } else {
            initializableBlockEntities.add(be);
        }
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void initialize(final CallbackInfo ci) {
        iteratingTickingBlockEntities = true;
        initializableBlockEntities.addAll(pendingInitializableBlockEntities);
        pendingInitializableBlockEntities.clear();
        for (final InitializableBlockEntity block : initializableBlockEntities) {
            block.onInit();
        }
        initializableBlockEntities.clear();
        iteratingTickingBlockEntities = false;
    }

    @Override
    public void unloadPiece(final long id, final MicroBlock<?> piece) {
        boolean anyLoaded = false;
        for (final BlockPos position : MicroWorldUtil.positions(piece.outlineShape())) {
            if (isChunkLoaded(ChunkSectionPos.getSectionCoord(position.getX()), ChunkSectionPos.getSectionCoord(position.getZ()))) {
                anyLoaded = true;
            }
        }
        if (!anyLoaded) {
            updateCanonicalPos(id, VoxelShapes.empty());
        }
    }

    @Override
    public Optional<MicroBlockRaycastResult> microRaycast(final Vec3d start, final Vec3d end, final MicroShapeType shapeType, final Predicate<FluidState> fluidHandler) {
        return MicroWorldUtil.raycast(start, end, shapeType, fluidHandler, (World) (Object) this);
    }
}
