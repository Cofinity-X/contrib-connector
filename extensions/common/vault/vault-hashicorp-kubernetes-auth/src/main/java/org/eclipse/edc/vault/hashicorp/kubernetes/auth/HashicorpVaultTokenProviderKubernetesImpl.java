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

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesAuthClient;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesLoginResponsePayload;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesSettings;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

public class HashicorpVaultTokenProviderKubernetesImpl implements HashicorpVaultTokenProvider {

    private String vaultToken = "";
    private String serviceAccountToken;
    private LocalDateTime tokenExpirationTimestamp;

    private final String vaultKubernetesAuthRole;
    private final HashicorpVaultKubernetesAuthClient authClient;
    private final HashicorpVaultKubernetesSettings settings;
    private final Monitor monitor;
    private final Clock clock;



    public HashicorpVaultTokenProviderKubernetesImpl(@NotNull HashicorpVaultKubernetesAuthClient authClient,
                                                     @NotNull HashicorpVaultKubernetesSettings settings,
                                                     @NotNull Monitor monitor,
                                                     @NotNull Clock clock,
                                                     @NotNull FileUtil fileUtil) {
        this.vaultKubernetesAuthRole = settings.getVaultK8sAuthRole();
        this.authClient = authClient;
        this.settings = settings;
        this.clock = clock;
        this.monitor = monitor;

        try {
            this.serviceAccountToken = fileUtil.readContentsFromFile(settings.getServiceAccountTokenPath());
        } catch (IOException ex) {
            this.serviceAccountToken = settings.getServiceAccountToken();
            monitor.warning(String.format("Failed reading service account token from local path [%s]", settings.getServiceAccountTokenPath()));
        }

        //do a first login to generate a token and the expiration timestamp
        login();
    }

    public void login() {
        Result<HashicorpVaultKubernetesLoginResponsePayload> res =
                authClient.loginWithKubernetesAuth(vaultKubernetesAuthRole, serviceAccountToken);

        if (res.succeeded()) {
            this.vaultToken = res.getContent().getAuth().getClientToken();
            this.tokenExpirationTimestamp = LocalDateTime.now(clock).plusSeconds(res.getContent().getAuth().getLeaseDuration());
        }

        if (res.failed()) {
            monitor.warning(String.format("Failed login with Kubernetes Service Account Token: (%s)", res.getFailureDetail()));
        }
    }

    public boolean shouldTokenBeRenewed() {
        if (tokenExpirationTimestamp == null) {
            return true;
        }

        LocalDateTime renewalThresholdDateTime = this.tokenExpirationTimestamp.minusSeconds(settings.getExpirationThresholdSeconds());
        return renewalThresholdDateTime.isBefore(LocalDateTime.now(clock));
    }

    public LocalDateTime getTokenExpirationTimestamp() {
        return tokenExpirationTimestamp;
    }

    @Override
    public String vaultToken() {
        if (shouldTokenBeRenewed()) {
            login();
        }
        return vaultToken;
    }

}
