package com.picvi.pet_companion_service.appversion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app/version-policy")
public class AppVersionController {
    private final AppVersionPolicy policy;

    @GetMapping
    public ResponseEntity<VersionPolicyResponse> get(@RequestParam String platform) {
        return AppPlatform.parse(platform)
                .map(this.policy::responseFor)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
