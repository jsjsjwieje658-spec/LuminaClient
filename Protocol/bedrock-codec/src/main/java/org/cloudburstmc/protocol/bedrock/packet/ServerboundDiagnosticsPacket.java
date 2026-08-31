package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.diagnostics.*;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class ServerboundDiagnosticsPacket implements BedrockPacket{
    public float avgFps;
    public float avgServerSimTickTimeMS;
    public float avgClientSimTickTimeMS;
    public float avgBeginFrameTimeMS;
    public float avgInputTimeMS;
    public float avgRenderTimeMS;
    public float avgEndFrameTimeMS;
    public float avgRemainderTimePercent;
    public float avgUnaccountedTimePercent;
    public final List<MemoryCategoryCounter> memoryCategoryValues = new ArrayList<>();
    /**
     * @since v975
     */
    public final List<EntityDiagnosticTimingInfo> entityDiagnostics = new ArrayList<>();
    /**
     * @since v975
     */
    public final List<SystemDiagnosticTimingInfo> systemDiagnostics = new ArrayList<>();
    /**
     * @since v1001
     */
    public final List<WhiskerScopeDataSummary> whiskerScopes = new ArrayList<>();
    /**
     * @since v2168
     */
    public final List<SystemCategory> systemCategories = new ArrayList<>();

    @Override
    public PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.SERVERBOUND_DIAGNOSTICS;
    }

    @Override
    public ServerboundDiagnosticsPacket clone() {
        try {
            return (ServerboundDiagnosticsPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
