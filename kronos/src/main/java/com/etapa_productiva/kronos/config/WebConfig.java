package com.etapa_productiva.kronos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Autowired
    private CambioContrasenaInterceptor cambioContrasenaInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Path.of(uploadRootDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(ubicacion);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cambioContrasenaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico");
    }
}
