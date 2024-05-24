/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret.v1;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.api.management.secret.BaseSecretsApiControllerTest;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;

class SecretsApiV1ControllerTest extends BaseSecretsApiControllerTest {
    @Override
    protected Object controller() {
        return new SecretsApiV1Controller(service, transformerRegistry, validator, mock());
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v1")
                .when();
    }
}