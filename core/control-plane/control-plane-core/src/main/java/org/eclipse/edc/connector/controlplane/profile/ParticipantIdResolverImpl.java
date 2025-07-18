package org.eclipse.edc.connector.controlplane.profile;

import org.eclipse.edc.protocol.spi.ParticipantIdResolver;

import java.util.HashMap;
import java.util.Map;

public class ParticipantIdResolverImpl implements ParticipantIdResolver {
    
    private final Map<String, String> idMap = new HashMap<>();
    private final String defaultParticipantId;

    public ParticipantIdResolverImpl(String defaultParticipantId) {
        this.defaultParticipantId = defaultParticipantId;
    }

    @Override
    public String resolveFor(String protocol) {
        if (idMap.containsKey(protocol)) {
            return idMap.get(protocol);
        }
        return defaultParticipantId;
    }

    @Override
    public void register(String protocol, String participantId) {
        idMap.put(protocol, participantId);
    }
}
