package org.eclipse.edc.protocol.spi;

public interface ParticipantIdResolver {
    
    /**
     * Resolves the participant identifier for the given protocol and returns it.
     *
     * @param protocol the protocol for which to resolve the participant identifier
     * @return the resolved participant identifier
     */
    String resolveFor(String protocol);
    
    /**
     * Registers a participant identifier for the given protocol.
     *
     * @param protocol the protocol
     * @param participantId the identifier
     */
    void register(String protocol, String participantId);
}
