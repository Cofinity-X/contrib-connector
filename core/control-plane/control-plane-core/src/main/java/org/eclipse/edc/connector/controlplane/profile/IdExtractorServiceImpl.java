package org.eclipse.edc.connector.controlplane.profile;

import org.eclipse.edc.protocol.spi.IdExtractorService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class IdExtractorServiceImpl implements IdExtractorService {
    
    //TODO remove
    
    static final String DEFAULT = "default";
    
    private final Map<String, Function<ClaimToken, String>> extractionFunctions = new HashMap<>();
    
    public IdExtractorServiceImpl(Function<ClaimToken, String> defaultExtractionFunction) {
        registerExtractionFunction(DEFAULT, defaultExtractionFunction);
    }
    
    @Override
    public Result<String> extractId(ClaimToken claimToken, String protocol) {
        var id = extractionFunctions.getOrDefault(protocol, extractionFunctions.get(DEFAULT))
                .apply(claimToken);
        
        if (id == null || id.isBlank()) {
            return Result.failure("Failed to extract ID for protocol: " + protocol);
        }
        
        return Result.success(id);
    }
    
    @Override
    public void registerExtractionFunction(String protocol, Function<ClaimToken, String> extractionFunction) {
        extractionFunctions.put(protocol, extractionFunction);
    }
    
    @Override
    public void overrideDefaultExtractionFunction(Function<ClaimToken, String> extractionFunction) {
        extractionFunctions.put(DEFAULT, extractionFunction);
    }
}
