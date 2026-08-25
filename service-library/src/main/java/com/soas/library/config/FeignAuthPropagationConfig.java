package com.soas.library.config;

import com.soas.library.security.AuthHeaders;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthPropagationConfig {
    @Bean
    public RequestInterceptor authHeaderPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            copyHeader(template, attributes, AuthHeaders.EMAIL);
            copyHeader(template, attributes, AuthHeaders.ROLE);
        };
    }

    private void copyHeader(feign.RequestTemplate template, ServletRequestAttributes attributes, String name) {
        String value = attributes.getRequest().getHeader(name);
        if (value != null && !value.isBlank() && !template.headers().containsKey(name)) {
            template.header(name, value);
        }
    }
}
