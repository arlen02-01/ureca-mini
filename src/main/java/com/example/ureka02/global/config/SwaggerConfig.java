package com.example.ureka02.global.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.title:Ureka API}")
    private String title;

    @Value("${swagger.description:Ureka02 미니 프로젝트 API 명세서}")
    private String description;

    @Value("${swagger.version:1.0.0}")
    private String version;

    // JWT 보안 설정
    private final SecurityScheme securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    static {
        SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime.class,
                new Schema<String>()
                        .type("string")
                        .format("date-time")
                        .example(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        SpringDocUtils.getConfig().replaceWithSchema(LocalDate.class,
                new Schema<String>()
                        .type("string")
                        .format("date")
                        .example(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));

        SpringDocUtils.getConfig().replaceWithSchema(LocalTime.class,
                new Schema<String>()
                        .type("string")
                        .format("time")
                        .example(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
    }

    @Bean
    public OpenAPI openApi() {
        String securityRequirementName = "JWT Auth";

        return new OpenAPI()

                .servers(Collections.singletonList(new Server().url("/")))

                .security(Collections.singletonList(new SecurityRequirement().addList(securityRequirementName)))
                .components(new Components().addSecuritySchemes(securityRequirementName, securityScheme))

                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                )
                .externalDocs(new ExternalDocumentation()
                        .description("팀 노션 페이지 바로가기")
                        .url("https://www.notion.so/2-2b7b3fd475e9806293bbd7dad2fdca0c"));
    }
}