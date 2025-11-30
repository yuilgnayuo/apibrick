package com.citi.tts.apibrick.model.swagger.response;

import com.citi.tts.apibrick.model.swagger.vo.ApiDetailVO;
import lombok.Data;

/**
 * API Detail Response Model
 */
@Data
public class ApiDetailResponse extends BaseResponse {
    
    /**
     * API detail data
     */
    private ApiDetailVO data;
}

