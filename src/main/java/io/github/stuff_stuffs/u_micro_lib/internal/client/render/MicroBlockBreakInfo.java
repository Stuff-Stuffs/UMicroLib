package io.github.stuff_stuffs.u_micro_lib.internal.client.render;

import org.jetbrains.annotations.NotNull;

public class MicroBlockBreakInfo implements Comparable<MicroBlockBreakInfo> {
    private final int actorNetworkId;
    private final long id;
    private int stage;
    public int lastUpdateTick;

    public MicroBlockBreakInfo(final int actorNetworkId, final long id) {
        this.actorNetworkId = actorNetworkId;
        this.id = id;
    }

    public void setStage(int stage) {
        if (stage >= 10) {
            stage = 9;
        }
        this.stage = stage;
    }

    public long id() {
        return id;
    }

    public int actorNetworkId() {
        return actorNetworkId;
    }

    public int stage() {
        return stage;
    }

    public int lastUpdateTick() {
        return lastUpdateTick;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MicroBlockBreakInfo that)) {
            return false;
        }

        return actorNetworkId == that.actorNetworkId;
    }

    @Override
    public int hashCode() {
        return actorNetworkId;
    }

    @Override
    public int compareTo(@NotNull final MicroBlockBreakInfo o) {
        return stage != o.stage ? Integer.compare(stage, o.stage) : Integer.compare(actorNetworkId, o.actorNetworkId);
    }
}
