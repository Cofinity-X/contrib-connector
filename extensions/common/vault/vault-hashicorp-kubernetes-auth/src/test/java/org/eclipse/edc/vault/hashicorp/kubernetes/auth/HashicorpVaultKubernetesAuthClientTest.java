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
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesAuthClient;
import org.eclipse.edc.vault.hashicorp.kubernetes.auth.client.HashicorpVaultKubernetesLoginResponsePayload;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;

public class HashicorpVaultKubernetesAuthClientTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void loginWithKubernetesAuth() throws IOException {
        // prepare
        String vaultUrl = "https://mock.url";
        String kubernetesRole = "role";
        String kubernetesServiceAccountToken = "serviceAccountToken";
        EdcHttpClient httpClient = Mockito.mock(EdcHttpClient.class);
        HashicorpVaultKubernetesAuthClient authClient = new HashicorpVaultKubernetesAuthClient(vaultUrl, httpClient, objectMapper);

        Response response = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);
        HashicorpVaultKubernetesLoginResponsePayload payload = new HashicorpVaultKubernetesLoginResponsePayload();

        Mockito.when(httpClient.execute(Mockito.any(Request.class))).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string()).thenReturn(payload.toString());

        // invoke
        Result<HashicorpVaultKubernetesLoginResponsePayload> result =
            authClient.loginWithKubernetesAuth(kubernetesRole, kubernetesServiceAccountToken);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(httpClient, Mockito.times(1))
            .execute(
                Mockito.argThat(
                    request ->
                        request.method().equalsIgnoreCase("POST")
                            && request.url().encodedPath().endsWith("v1%2Fauth%2Fkubernetes%2Flogin")));
    }

}
