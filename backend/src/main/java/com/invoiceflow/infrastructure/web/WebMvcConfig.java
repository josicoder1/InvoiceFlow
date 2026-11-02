package com.invoiceflow.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path backendUploads = Paths.get("uploads").toAbsolutePath().normalize();
        Path parentUploads = Paths.get("../uploads").toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        backendUploads.toUri().toString() + "/",
                        parentUploads.toUri().toString() + "/"
                );
    }
}
