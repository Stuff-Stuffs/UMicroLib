package io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.InitializableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.api.common.block.UnloadableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.client.render.model.MicroBlockContainerRenderAttachment;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.LongStream;

public class MicroBlockContainerBlockEntity extends BlockEntity implements InitializableBlockEntity, UnloadableBlockEntity, RenderAttachmentBlockEntity {
    private final Container container;
    private PartialContainer partialContainer;
    private boolean unloaded = false;
    private boolean initialized = false;

    public MicroBlockContainerBlockEntity(final BlockPos pos, final BlockState state) {
        super(UMicroLibBlockEntities.MULTI_PART_BLOCK_ENTITY_TYPE, pos, state);
        container = new Container();
    }

    public Container getContainer() {
        return container;
    }

    public void sync() {
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(getPos());
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        final NbtCompound compound = new NbtCompound();
        writeNbt(compound);
        return compound;
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        partialContainer = new PartialContainer();
        final NbtList list = nbt.getList("pieces", NbtElement.COMPOUND_TYPE);
        for (final NbtElement element : list) {
            final NbtCompound entry = (NbtCompound) element;
            final Vec3d pos = new Vec3d(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"));
            final MicroBlockType<?, ?> type = MicroBlockType.REGISTRY.get(new Identifier(entry.getString("type")));
            if (type != null) {
                final MicroBlock<?> piece = type.create(pos);
                piece.readFromNbt(entry.getCompound("data"));
                partialContainer.pieces.put(entry.getLong("id"), piece);
            }
        }
        if (world != null && initialized) {
            partialContainer.build(world);
            partialContainer = null;
            if (container.pieces.isEmpty()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
    }

    @Override
    protected void writeNbt(final NbtCompound nbt) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) world;
        final NbtList list = new NbtList();
        for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : container.pieces.long2ObjectEntrySet()) {
            final BlockPos blockPos = extensions.canonicalPos(new MicroBlockHandleImpl(world.getRegistryKey(), entry.getLongKey()));
            if (blockPos != null && blockPos.equals(pos)) {
                final NbtCompound compound = new NbtCompound();
                final NbtCompound child = new NbtCompound();
                final MicroBlock<?> piece = entry.getValue();
                piece.writeToNbt(child);
                compound.put("data", child);
                compound.putLong("id", entry.getLongKey());
                compound.putString("type", MicroBlockType.REGISTRY.getId(piece.type()).toString());
                final Vec3d pos = piece.pos();
                compound.putDouble("x", pos.x);
                compound.putDouble("y", pos.y);
                compound.putDouble("z", pos.z);
                list.add(compound);
            }
        }
        nbt.put("pieces", list);
    }

    @Override
    public void onInit() {
        if (partialContainer != null) {
            partialContainer.build(world);
            partialContainer = null;
            initialized = true;
            if (container.pieces.isEmpty()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (!unloaded) {
            for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : container.pieces.long2ObjectEntrySet()) {
                ((MicroBlockWorldExtensions) world).breakPiece(entry.getLongKey(), entry.getValue());
            }
            unloaded = true;
        }
    }

    @Override
    public void onUnload() {
        for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : container.pieces.long2ObjectEntrySet()) {
            ((MicroBlockWorldExtensions) world).unloadPiece(entry.getLongKey(), entry.getValue());
        }
        unloaded = true;
    }

    @Override
    public @Nullable Object getRenderAttachmentData() {
        final Long2ObjectMap<MicroBlockContainerRenderAttachment.Entry<?>> data = new Long2ObjectOpenHashMap<>();
        for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : container.pieces.long2ObjectEntrySet()) {
            if (((MicroBlockWorldExtensions) world).canonicalPos(new MicroBlockHandleImpl(world.getRegistryKey(), entry.getLongKey())).equals(pos)) {
                addData(data, entry.getLongKey(), entry.getValue());
            }
        }
        return new MicroBlockContainerRenderAttachment(data);
    }

    private static <T> void addData(final Long2ObjectMap<MicroBlockContainerRenderAttachment.Entry<?>> map, final long key, final MicroBlock<T> block) {
        map.put(key, new MicroBlockContainerRenderAttachment.Entry<>(block.type(), block.pos(), block.clientData()));
    }

    private static final class PartialContainer {
        private final Long2ObjectMap<MicroBlock<?>> pieces = new Long2ObjectOpenHashMap<>();

        public void build(final World world) {
            for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                if (!((MicroBlockWorldExtensions) world).tryLoadPiece(new MicroBlockHandleImpl(world.getRegistryKey(), entry.getLongKey()), entry.getValue())) {
                    entry.getValue().drop(world);
                }
            }
        }
    }


    public final class Container {
        private final Long2ObjectMap<MicroBlock<?>> pieces = new Long2ObjectOpenHashMap<>();
        private VoxelShape collisionShape = VoxelShapes.empty();
        private VoxelShape outlineShape = VoxelShapes.empty();

        public LongStream canonPieces() {
            return pieces.long2ObjectEntrySet().stream().filter(entry -> pos.equals(((MicroBlockWorldExtensions) world).canonicalPos(new MicroBlockHandleImpl(world.getRegistryKey(), entry.getLongKey())))).mapToLong(Long2ObjectMap.Entry::getLongKey);
        }

        public LongSet pieces() {
            return LongSets.unmodifiable(pieces.keySet());
        }

        public MicroBlock<?> byId(final long id) {
            return pieces.get(id);
        }

        public VoxelShape collisionShape() {
            return collisionShape;
        }

        public VoxelShape outlineShape() {
            return outlineShape;
        }

        public VoxelShape outlineShapeWithout(final long id) {
            if (!pieces.containsKey(id)) {
                return outlineShape;
            } else {
                VoxelShape shape = VoxelShapes.empty();
                for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                    if (entry.getLongKey() != id) {
                        shape = VoxelShapes.combine(shape, entry.getValue().outlineShape(), BooleanBiFunction.OR);
                    }
                }
                return shape;
            }
        }

        public void removePiece(final long id) {
            if (pieces.remove(id) != null) {
                if (pieces.isEmpty()) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    return;
                }
                VoxelShape shape = VoxelShapes.empty();
                for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                    shape = VoxelShapes.combine(shape, entry.getValue().outlineShape(), BooleanBiFunction.OR);
                }
                final VoxelShape clipper = VoxelShapes.fullCube().offset(pos.getX(), pos.getY(), pos.getZ());
                outlineShape = clip(shape, clipper).simplify();
                shape = VoxelShapes.empty();
                for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                    shape = VoxelShapes.combine(shape, entry.getValue().collisionShape(), BooleanBiFunction.OR);
                }
                collisionShape = clip(shape, clipper).simplify();
                sync();
            }
        }

        public void outlineShapeChange(final long id, final MicroBlock<?> piece, final VoxelShape newShape) {
            if (VoxelShapes.matchesAnywhere(newShape, VoxelShapes.fullCube().offset(pos.getX(), pos.getY(), pos.getZ()), BooleanBiFunction.AND)) {
                pieces.put(id, piece);
                VoxelShape shape = newShape;
                for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                    if (entry.getLongKey() != id) {
                        shape = VoxelShapes.combine(shape, entry.getValue().outlineShape(), BooleanBiFunction.OR);
                    }
                }
                outlineShape = clip(shape, VoxelShapes.fullCube().offset(pos.getX(), pos.getY(), pos.getZ())).simplify();
                sync();
            } else {
                if (pieces.remove(id) != null) {
                    if (!pieces.isEmpty()) {
                        VoxelShape shape = VoxelShapes.empty();
                        for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                            shape = VoxelShapes.combine(shape, entry.getValue().outlineShape(), BooleanBiFunction.OR);
                        }
                        outlineShape = clip(shape, VoxelShapes.fullCube().offset(pos.getX(), pos.getY(), pos.getZ())).simplify();
                        sync();
                    } else {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        public void collisionShapeChange(final long id, final MicroBlock<?> piece, final VoxelShape newShape) {
            if (pieces.containsKey(id)) {
                pieces.put(id, piece);
                VoxelShape shape = newShape;
                for (final Long2ObjectMap.Entry<MicroBlock<?>> entry : pieces.long2ObjectEntrySet()) {
                    if (entry.getLongKey() != id) {
                        shape = VoxelShapes.combine(shape, entry.getValue().collisionShape(), BooleanBiFunction.OR);
                    }
                }
                collisionShape = shape.simplify();
                sync();
            }
        }

        private VoxelShape clip(final VoxelShape subject, final VoxelShape clipper) {
            return VoxelShapes.combine(subject, clipper, BooleanBiFunction.AND);
        }

        public Optional<VoxelShape> rayCastShape(final double x, final double y, final double z, final Vec3d dir, final double length) {
            return rayCastId(x, y, z, dir, length).stream().mapToObj(id -> pieces.get(id).outlineShape()).findFirst();
        }

        public OptionalLong rayCastId(final double x, final double y, final double z, final Vec3d dir, final double length) {
            final Vec3d start = new Vec3d(x, y, z);
            final Vec3d end = start.add(dir.multiply(length));
            double distSq = Double.POSITIVE_INFINITY;
            Long best = null;
            for (final Long2ObjectMap.Entry<MicroBlock<?>> piece : pieces.long2ObjectEntrySet()) {
                final BlockHitResult result = piece.getValue().outlineShape().raycast(start, end, BlockPos.ORIGIN);
                if (result != null) {
                    final Vec3d pos = result.getPos();
                    final double v = pos.squaredDistanceTo(x, y, z);
                    if (v < distSq) {
                        distSq = v;
                        best = piece.getLongKey();
                    }
                }
            }
            if (best == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(best);
        }
    }
}
