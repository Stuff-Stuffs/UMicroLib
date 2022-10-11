package io.github.stuff_stuffs.u_micro_lib.internal.client.render.model;

import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockModel;
import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockRenderer;
import io.github.stuff_stuffs.u_micro_lib.impl.render.MicroBlockRendererRegistryImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.client.UMicroLibClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class MicroBlockContainerModel implements BakedModel, FabricBakedModel {
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(final BlockRenderView blockView, final BlockState state, final BlockPos pos, final Supplier<Random> randomSupplier, final RenderContext context) {
        final Object attachment = ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(pos);
        if (!(attachment instanceof MicroBlockContainerRenderAttachment renderAttachment)) {
            throw new RuntimeException("Render attachment data mismatch!");
        }
        if (UMicroLibClient.DAMAGE_BLOCK.isEmpty()) {
            for (final Long2ObjectMap.Entry<MicroBlockContainerRenderAttachment.Entry<?>> entry : renderAttachment.entriesById().long2ObjectEntrySet()) {
                render(pos, entry.getValue(), blockView, randomSupplier, context);
            }
        } else {
            final LongIterator iterator = UMicroLibClient.DAMAGE_BLOCK.iterator();
            while (iterator.hasNext()) {
                final long id = iterator.nextLong();
                final MicroBlockContainerRenderAttachment.Entry<?> entry = renderAttachment.entriesById().get(id);
                if (entry != null) {
                    render(pos, entry, blockView, randomSupplier, context);
                }
            }
        }
    }

    private <ClientData> void render(final BlockPos pos, final MicroBlockContainerRenderAttachment.Entry<ClientData> entry, final BlockRenderView blockView, final Supplier<Random> randomSupplier, final RenderContext context) {
        final MicroBlockRenderer<ClientData> renderer = MicroBlockRendererRegistryImpl.INSTANCE.renderer(entry.type());
        if (renderer.hasBakedModel()) {
            final Vec3f vec = new Vec3f();
            final Vec3d blockPos = entry.pos();
            final double dx = blockPos.x - pos.getX();
            final double dy = blockPos.y - pos.getY();
            final double dz = blockPos.z - pos.getZ();
            final Vec3f offset = new Vec3f((float) dx, (float) dy, (float) dz);
            final MicroBlockModel<ClientData> model = renderer.bakedModel(entry.data());
            context.pushTransform(quad -> {
                for (int i = 0; i < 4; i++) {
                    quad.copyPos(i, vec);
                    vec.add(offset);
                    quad.pos(i, vec);
                }
                return true;
            });
            model.renderBlockQuads(blockView, entry.data(), randomSupplier, context);
            context.popTransform();
        }
    }

    @Override
    public void emitItemQuads(final ItemStack stack, final Supplier<Random> randomSupplier, final RenderContext context) {

    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction face, final Random random) {
        throw new UnsupportedOperationException("FRAPI not present, you probably need Indium!");
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return null;
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}
