package com.example.ureka02.global.controller;

import com.example.ureka02.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Operation(summary = "헬스체크", description = "서버 상태를 확인합니다.")
    @GetMapping
    public ResponseDto<String> healthCheck() {
        return ResponseDto.ok("OK");
    }
}
