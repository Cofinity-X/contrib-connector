/*
 *  Copyright (c) 2025 Cofinity-X GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.kubernetes.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesAuthClient;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesSettings;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.util.FileUtil;

import java.time.Clock;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Extension(value = HashicorpVaultKubernetesAuthExtension.NAME)
public class HashicorpVaultKubernetesAuthExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Vault Kubernetes Auth";

    @Inject
    private EdcHttpClient httpClient;

    @Configuration
    private HashicorpVaultKubernetesSettings config;

    private HashicorpVaultKubernetesAuthClient client;
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public HashicorpVaultTokenProvider tokenProvider() {
        return new HashicorpVaultTokenProviderKubernetesImpl(
                hashicorpVaultTokenAuthClient(),
                config,
                monitor,
                Clock.systemUTC(),
                new FileUtil());
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix(NAME);
    }

    private HashicorpVaultKubernetesAuthClient hashicorpVaultTokenAuthClient() {
        if (client == null) {
            var mapper = new ObjectMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

            client = new HashicorpVaultKubernetesAuthClient(
                    config.getVaultUrl(),
                    httpClient,
                    mapper);
        }
        return client;
    }

}
