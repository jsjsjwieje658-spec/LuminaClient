package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.List;

/**
 * Sends a set of update properties for the texture shift system from the server to the client.
 *
 * @since v924
 */
@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class ClientboundTextureShiftPacket implements BedrockPacket {

    public Action action;
    public String collectionName;
    public String fromStep;
    public String toStep;
    public List<String> allSteps;
    public long currentLengthTicks;
    public long totalLengthTicks;
    public boolean enabled;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.CLIENTBOUND_TEXTURE_SHIFT;
    }

    @Override
    public ClientboundTextureShiftPacket clone() {
        try {
            return (ClientboundTextureShiftPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public enum Action {
        INVALID,
        INITIALIZE,
        START,
        SET_ENABLED,
        SYNC
    }
}
