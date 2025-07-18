package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

import java.util.function.Function;

public interface IdExtractorService {
    
    //TODO remove
    
    Result<String> extractId(ClaimToken claimToken, String protocol);
    
    void registerExtractionFunction(String protocol, Function<ClaimToken, String> extractionFunction);
    
    void overrideDefaultExtractionFunction(Function<ClaimToken, String> extractionFunction);
}
