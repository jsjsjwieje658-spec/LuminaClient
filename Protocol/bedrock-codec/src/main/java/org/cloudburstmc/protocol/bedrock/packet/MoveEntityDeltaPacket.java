package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.EnumSet;
import java.util.Set;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
public class MoveEntityDeltaPacket implements BedrockPacket {
    public long runtimeEntityId;

    public final Set<Flag> flags = EnumSet.noneOf(Flag.class);

    @Deprecated
    public int deltaX;
    @Deprecated
    public int deltaY;
    @Deprecated
    public int deltaZ;

    public float x;
    public float y;
    public float z;

    public float pitch;
    public float yaw;
    public float headYaw;

    public boolean onGround;
    public boolean forceMove;
    public boolean forceMoveLocalEntity;
    public boolean forceCompletion;

    /**
     * @since v2192
     */
    public long ticks;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.MOVE_ENTITY_DELTA;
    }

    public String toString() {
        return "MoveEntityDeltaPacket(runtimeEntityId=" + runtimeEntityId +
                ", flags=" + flags + ", delta=(" + deltaX + ", " + deltaY + ", " + deltaZ +
                "), position=(" + x + ", " + y + ", " + z +
                "), rotation=(" + pitch + ", " + yaw + ", " + headYaw + "), onGround=" + onGround +",forceMove=" +
                forceMove + ",forceMoveLocalEntity=" + forceMoveLocalEntity + ",forceCompletion=" + forceCompletion + ")";
    }

    public enum Flag {
        HAS_X,
        HAS_Y,
        HAS_Z,
        HAS_PITCH,
        HAS_YAW,
        HAS_HEAD_YAW,
        ON_GROUND,
        TELEPORTING,
        FORCE_MOVE_LOCAL_ENTITY,
        FORCE_COMPLETION
    }

    @Override
    public MoveEntityDeltaPacket clone() {
        try {
            return (MoveEntityDeltaPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

