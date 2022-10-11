package io.github.stuff_stuffs.u_micro_lib.api.client.render;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface MicroBlockModel<ClientData> {
    void renderBlockQuads(BlockRenderView blockView, ClientData data, Supplier<Random> randomSupplier, RenderContext context);

    static <ClientData> MicroBlockModel<ClientData> basic(final Supplier<BakedModel> modelSupplier) {
        return basic(modelSupplier, BlendMode.SOLID);
    }

    static <ClientData> MicroBlockModel<ClientData> basic(final Supplier<BakedModel> modelSupplier, final BlendMode blendMode) {
        if (!RendererAccess.INSTANCE.hasRenderer()) {
            throw new RuntimeException("FRAPI is not present, you probably need the Indium mod!");
        }
        return basic(modelSupplier, RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, blendMode).find());
    }

    static <ClientData> MicroBlockModel<ClientData> basic(final Supplier<BakedModel> modelSupplier, final RenderMaterial material) {
        return (blockView, data, randomSupplier, context) -> {
            final BakedModel model = modelSupplier.get();
            if (model instanceof FabricBakedModel fabricBakedModel && !fabricBakedModel.isVanillaAdapter()) {
                throw new UnsupportedOperationException("Must use advanced micro block model adapter to use FRAPI!");
            }
            final QuadEmitter emitter = context.getEmitter();
            final List<BakedQuad> quads = model.getQuads(null, null, randomSupplier.get());
            for (final BakedQuad quad : quads) {
                emitter.fromVanilla(quad, material, null);
                emitter.emit();
            }
        };
    }

    static <ClientData> MicroBlockModel<ClientData> stateMapping(final Supplier<BakedModel> modelSupplier, final Function<ClientData, BlockState> stateMapper, final Function<ClientData, BlockPos> positionMapper) {
        return stateMapping(modelSupplier, stateMapper, positionMapper, BlendMode.SOLID);
    }

    static <ClientData> MicroBlockModel<ClientData> stateMapping(final Supplier<BakedModel> modelSupplier, final Function<ClientData, BlockState> stateMapper, final Function<ClientData, BlockPos> positionMapper, final BlendMode blendMode) {
        if (!RendererAccess.INSTANCE.hasRenderer()) {
            throw new RuntimeException("FRAPI is not present, you probably need the Indium mod!");
        }
        return stateMapping(modelSupplier, stateMapper, positionMapper, RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, blendMode).find());
    }

    static <ClientData> MicroBlockModel<ClientData> stateMapping(final Supplier<BakedModel> modelSupplier, final Function<ClientData, BlockState> stateMapper, final Function<ClientData, BlockPos> positionMapper, final RenderMaterial material) {
        return (blockView, data, randomSupplier, context) -> {
            final BakedModel model = modelSupplier.get();
            final BlockState state = stateMapper.apply(data);
            if (model instanceof FabricBakedModel fabricBakedModel && !fabricBakedModel.isVanillaAdapter()) {
                fabricBakedModel.emitBlockQuads(blockView, state, positionMapper.apply(data), randomSupplier, context);
            } else {
                final QuadEmitter emitter = context.getEmitter();
                final List<BakedQuad> quads = model.getQuads(state, null, randomSupplier.get());
                for (final BakedQuad quad : quads) {
                    emitter.fromVanilla(quad, material, null);
                    emitter.emit();
                }
            }
        };
    }
}
