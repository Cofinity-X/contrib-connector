/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * A registry of configured {@link DataspaceProfileContext}.
 * Profile contexts can be of 2 types: default and standard.
 * <p>
 * Default ones are available out of the box from the EDC libraries.
 * Standard ones can be defined and register by adopters following their Dataspace peculiarities.
 * <p>
 * A single "standard" registration will hide all the default ones,
 */
public interface DataspaceProfileContextRegistry {
    
    //TODO should participantIdResolver be integrated into the registry? would make sense

    /**
     * Register a default profile context. Will use the default mechanism for ID extraction.
     *
     * @param context the default profile context.
     */
    void registerDefault(DataspaceProfileContext context);

    /**
     * Register a standard profile context. Will use the default mechanism for ID extraction.
     *
     * @param context the standard profile context.
     */
    void register(DataspaceProfileContext context);
    
    /**
     * Register a standard profile context.
     *
     * @param context the standard profile context.
     * @param idExtractionFunction the function for extracting the participant ID from a {@link ClaimToken}.
     */
    void register(DataspaceProfileContext context, Function<ClaimToken, String> idExtractionFunction);
    
    //TODO Javadoc
    //TODO is this required on default profiles as well? probably not, as for any custom protocol version standard profile would be used?
    void overrideDefaultIdExtractionFunction(Function<ClaimToken, String> extractionFunction);

    /**
     * get all the protocol versions.
     *
     * @return the protocol versions.
     */
    ProtocolVersions getProtocolVersions();

    /**
     * Resolve a webhook for a protocol.
     *
     * @param protocol The protocol
     * @return The webhook for the protocol, or null if no webhook is registered for the protocol
     */
    @Nullable
    ProtocolWebhook getWebhook(String protocol);


    /**
     * Get the protocol version for a given protocol.
     *
     * @param protocol The protocol name
     * @return The protocol version, or null if no version is registered for the protocol
     */
    @Nullable
    ProtocolVersion getProtocolVersion(String protocol);
    
    //TODO Javadoc
    Function<ClaimToken, String> getIdExtractionFunction(String protocol);
}
