package com.aidb.aidb_backend.config.api;

import com.aidb.aidb_backend.model.api.APIResponse;
import com.aidb.aidb_backend.model.api.PayloadMetadata;
import com.aidb.aidb_backend.model.api.TierInfo;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// Advisor adds Payload Metadata to response data to form formatted response payload.
public class ApiResponseAdvisor implements ResponseBodyAdvice<Object> {

    private final TierInfo tierInfo;

    public ApiResponseAdvisor(TierInfo tierInfo) {
        this.tierInfo = tierInfo;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> coverterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        PayloadMetadata meta = new PayloadMetadata(tierInfo);
        return new APIResponse<>(meta, body);
    }
}
