package io.github.stuff_stuffs.u_micro_lib.internal.common.network;

import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Iterator;

public class MicroBlockBreakSender {
    public static final Identifier CHANNEL = UMicroLibCommon.id("break_progress_channel");

    public static void send(int entityId, final long id, final int stage, final Collection<ServerPlayerEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        buf.writeLong(id);
        buf.writeVarInt(stage);
        buf.retain();
        final Iterator<ServerPlayerEntity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            final ServerPlayerEntity next = iterator.next();
            final PacketSender sender = ServerPlayNetworking.getSender(next);
            if (iterator.hasNext()) {
                sender.sendPacket(CHANNEL, buf);
            } else {
                sender.sendPacket(CHANNEL, buf, PacketCallbacks.always(buf::release));
            }
        }
    }

    private MicroBlockBreakSender() {
    }
}
