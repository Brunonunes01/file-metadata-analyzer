package com.metascan.controller;

import com.metascan.dto.osint.UsernameScanResponseDto;
import com.metascan.service.UsernameScanService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/osint")
public class OsintController {

    private final UsernameScanService usernameScanService;

    public OsintController(UsernameScanService usernameScanService) {
        this.usernameScanService = usernameScanService;
    }

    @PostMapping("/username/scan")
    public ResponseEntity<UsernameScanResponseDto> scanUsername(
            @RequestParam("username") @NotBlank String username
    ) {
        UsernameScanResponseDto response = usernameScanService.scanUsername(username);
        return ResponseEntity.ok(response);
    }
}
