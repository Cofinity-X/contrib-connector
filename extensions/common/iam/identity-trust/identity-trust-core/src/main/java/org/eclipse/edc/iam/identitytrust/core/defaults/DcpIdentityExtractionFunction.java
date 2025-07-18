package org.eclipse.edc.iam.identitytrust.core.defaults;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.core.DcpDefaultServicesExtension.CLAIMTOKEN_VC_KEY;

public class DcpIdentityExtractionFunction implements Function<ClaimToken, String> {
    @Override
    public String apply(ClaimToken claimToken) {
        return ofNullable(claimToken.getListClaim(CLAIMTOKEN_VC_KEY)).orElse(emptyList())
                .stream()
                .filter(o -> o instanceof VerifiableCredential)
                .map(o -> (VerifiableCredential) o)
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .map(CredentialSubject::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
