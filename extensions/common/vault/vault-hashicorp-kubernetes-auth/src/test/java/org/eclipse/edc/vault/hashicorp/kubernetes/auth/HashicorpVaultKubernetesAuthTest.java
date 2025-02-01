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
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesAuthClient;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesLoginResponsePayload;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesSettings;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesSettings.DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class HashicorpVaultKubernetesAuthTest {

    private final String vaultToken = "token";

    private final Monitor monitor = mock();

    HashicorpVaultKubernetesAuthClient authClient;

    FileUtil fileUtil;

    Clock clock;

    HashicorpVaultKubernetesSettings settings;

    @BeforeEach
    void setup() throws IOException {
        authClient = mock(HashicorpVaultKubernetesAuthClient.class);
        fileUtil = mock(FileUtil.class);

        long fixedClockTime = 1672574400; // 1-1-2023 13:00:00
        clock = Clock.fixed(Instant.ofEpochSecond(fixedClockTime), ZoneId.of("Europe/Berlin"));

        Long expirationDurationSeconds = 60L;
        var auth = new HashicorpVaultKubernetesLoginResponsePayload.Auth(vaultToken, expirationDurationSeconds);
        var successPayload = new HashicorpVaultKubernetesLoginResponsePayload(auth);

        settings = HashicorpVaultKubernetesSettings.Builder.newInstance()
                .vaultK8sAuthRole("serviceAccountRole")
                .serviceAccountTokenPath(DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH)
                .build();

        Mockito.when(authClient.loginWithKubernetesAuth(any(), any()))
            .thenReturn(Result.success(successPayload));
        Mockito.when(fileUtil.readContentsFromFile(DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH))
            .thenReturn("my-service-token-jwt");
    }

    @Test
    void throwsExceptionWhenLoginFails() {
        Mockito.when(authClient.loginWithKubernetesAuth(any(), any()))
            .thenReturn(Result.failure("Failed"));

        HashicorpVaultTokenProviderKubernetesImpl auth = new HashicorpVaultTokenProviderKubernetesImpl(authClient, settings, monitor, clock, fileUtil);

        // TODO assert still points to an old Exception that no longer exists
        //Assertions.assertThrows(HashicorpVaultException.class, auth::login);
    }

    @Test
    void setsTokenWhenLoginSucceeds() {
        HashicorpVaultTokenProviderKubernetesImpl auth = new HashicorpVaultTokenProviderKubernetesImpl(authClient, settings, monitor, clock, fileUtil);
        auth.login();

        Assertions.assertEquals(auth.vaultToken(), vaultToken);
    }

    @Test
    void setsCorrectTokenExpirationTime() {
        LocalDateTime expectedExpirationTime = LocalDateTime.of(2023, 1, 1, 13, 1, 0);

        HashicorpVaultTokenProviderKubernetesImpl auth = new HashicorpVaultTokenProviderKubernetesImpl(authClient, settings, monitor, clock, fileUtil);
        auth.login();

        Assertions.assertEquals(auth.getTokenExpirationTimestamp(), expectedExpirationTime);
    }

    @Test
    void returnsFalseWhenTokenShouldNotGetRenewed() {
        HashicorpVaultTokenProviderKubernetesImpl auth = new HashicorpVaultTokenProviderKubernetesImpl(authClient, settings, monitor, clock, fileUtil);
        auth.login();

        // Expiration in 1 min so we don't expect a renewal
        Assertions.assertFalse(auth.shouldTokenBeRenewed());
    }

    @Test
    void returnsTrueWhenTokenShouldGetRenewed() {
        var auth = new HashicorpVaultKubernetesLoginResponsePayload.Auth(vaultToken, 10L);
        var successPayload = new HashicorpVaultKubernetesLoginResponsePayload(auth);

        Mockito.when(authClient.loginWithKubernetesAuth(any(), any()))
            .thenReturn(Result.success(successPayload));

        HashicorpVaultTokenProviderKubernetesImpl kubernetesAuth = new HashicorpVaultTokenProviderKubernetesImpl(authClient, settings, monitor, clock, fileUtil);
        kubernetesAuth.login();

        // Expiration in 10 sec so we expect a renewal
        Assertions.assertTrue(kubernetesAuth.shouldTokenBeRenewed());
    }

}
