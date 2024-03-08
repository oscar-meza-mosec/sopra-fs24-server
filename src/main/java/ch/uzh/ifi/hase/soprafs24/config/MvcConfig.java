package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    public MvcConfig() {
        super();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("addResourceHandlers!!!");
        registry.addResourceHandler("/static/**").addResourceLocations("/react/static/");
        registry.addResourceHandler("/manifest.json").addResourceLocations("/react/manifest.json");
        registry.addResourceHandler("/asset-manifest.json").addResourceLocations("/react/asset-manifest.json");
        registry.addResourceHandler("/favicon.ico").addResourceLocations("/react/favicon.ico");
    }

}