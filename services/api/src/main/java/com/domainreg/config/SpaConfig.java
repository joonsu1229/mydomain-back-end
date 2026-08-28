package com.domainreg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA(HTML5 history 모드) fallback.
 *
 * 프론트가 hash 모드(`/#/dashboard`)가 아닌 history 모드(`/dashboard`)로
 * 라우팅하므로, `/dashboard` 처럼 서버에 실제 파일이 없는 경로로 직접 접근해도
 * index.html을 내려줘야 한다. 그래야 브라우저가 Vue 앱을 부팅한 뒤
 * Vue Router가 URL 경로를 읽어 해당 화면을 그린다.
 *
 * 동작:
 *   - 정적 파일이 존재하면 그대로 서빙 (`/assets/**`, `/favicon.svg`, `/index.html`)
 *   - 존재하지 않으면 index.html로 폴백
 *   - `/api/**`는 컨트롤러 매핑이 우선하므로 여기서 건드리지 않음
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requested = location.createRelative(resourcePath);
                    return requested.exists() && requested.isReadable()
                        ? requested
                        : new ClassPathResource("/static/index.html");
                }
            });
    }
}
