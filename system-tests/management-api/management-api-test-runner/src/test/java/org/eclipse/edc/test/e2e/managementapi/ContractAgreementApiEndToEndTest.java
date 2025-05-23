/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e.managementapi;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ContractAgreementApiEndToEndTest {

    abstract static class Tests {

        @Test
        void getAll(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());
            store.save(createContractNegotiationBuilder("cn2").contractAgreement(createContractAgreement("cn2")).build());

            var jsonPath = context.baseRequest()
                    .contentType(JSON)
                    .post("/v3/contractagreements/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].assetId")).isNotNull();
            assertThat(jsonPath.getString("[1].assetId")).isNotNull();
            assertThat(jsonPath.getString("[0].@id")).isIn("cn1", "cn2");
            assertThat(jsonPath.getString("[1].@id")).isIn("cn1", "cn2");
        }

        @Test
        void getById(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            var agreement = createContractAgreement("cn1");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v3/contractagreements/cn1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(agreement.getId()))
                    .body("assetId", notNullValue())
                    .body("policy.'odrl:assignee'", is(agreement.getPolicy().getAssignee()))
                    .body("policy.'odrl:assigner'", is(agreement.getPolicy().getAssigner()));

        }

        @Test
        void getByIdV4Alpha(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            var agreement = createContractAgreement("cn1");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4alpha/contractagreements/cn1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(agreement.getId()))
                    .body("assetId", notNullValue())
                    .body("policy.'odrl:assignee'.'@id'", is(agreement.getPolicy().getAssignee()))
                    .body("policy.'odrl:assigner'.'@id'", is(agreement.getPolicy().getAssigner()));
        }

        @Test
        void getNegotiationByAgreementId(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("negotiation-id")
                    .contractAgreement(createContractAgreement("agreement-id"))
                    .build());

            var json = context.baseRequest()
                    .contentType(JSON)
                    .get("/v3/contractagreements/agreement-id/negotiation")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath();

            assertThat(json.getString("@id")).isEqualTo("negotiation-id");
        }

        private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
            return ContractNegotiation.Builder.newInstance()
                    .id(negotiationId)
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                            .uri("local://test")
                            .events(Set.of("test-event1", "test-event2"))
                            .build()))
                    .protocol("dataspace-protocol-http")
                    .contractOffer(contractOfferBuilder().build())
                    .state(FINALIZED.code());
        }

        private ContractOffer.Builder contractOfferBuilder() {
            return ContractOffer.Builder.newInstance()
                    .id("test-offer-id")
                    .assetId("test-asset-id")
                    .policy(Policy.Builder.newInstance().build());
        }

        private ContractAgreement createContractAgreement(String negotiationId) {
            return ContractAgreement.Builder.newInstance()
                    .id(negotiationId)
                    .assetId(UUID.randomUUID().toString())
                    .consumerId(UUID.randomUUID() + "-consumer")
                    .providerId(UUID.randomUUID() + "-provider")
                    .policy(Policy.Builder.newInstance().assignee("assignee").assigner("assigner").build())
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static ManagementEndToEndExtension runtime = new ManagementEndToEndExtension.Postgres(postgres);

    }
}
