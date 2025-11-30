package com.citi.tts.apibrick.model.swagger.response;

import com.citi.tts.apibrick.model.swagger.vo.ApiSimpleVO;
import lombok.Data;

import java.util.List;

/**
 * Pagination Query Response Model
 */
@Data
public class ApiPageResponse extends BaseResponse {
    
    /**
     * Pagination data
     */
    private PageData data;
    
    /**
     * Pagination data structure
     */
    @Data
    public static class PageData {
        /**
         * Total number of records
         */
        private Long total;
        
        /**
         * Total number of pages
         */
        private Integer pages;
        
        /**
         * Current page number
         */
        private Integer current;
        
        /**
         * Number of records per page
         */
        private Integer size;
        
        /**
         * List of API records
         */
        private List<ApiSimpleVO> records;
    }
}

