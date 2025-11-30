package com.citi.tts.apibrick.model.swagger.response;

import lombok.Data;

/**
 * Create API Response Model
 */
@Data
public class ApiCreateResponse extends BaseResponse {
    
    /**
     * Created API data
     */
    private CreateApiData data;
    
    /**
     * Created API data structure
     */
    @Data
    public static class CreateApiData {
        /**
         * Unique identifier of the newly created API
         */
        private String apiId;
    }
}

