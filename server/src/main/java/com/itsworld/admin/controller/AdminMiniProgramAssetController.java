package com.itsworld.admin.controller;

import com.itsworld.admin.service.MiniProgramAssetService;
import com.itsworld.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/miniprogram-assets")
@RequiredArgsConstructor
public class AdminMiniProgramAssetController {
    private final MiniProgramAssetService assets;

    @PostMapping
    public Result<MiniProgramAssetService.Asset> upload(
            @RequestParam MultipartFile file, @RequestParam(defaultValue = "onboarding") String purpose) {
        return Result.success(assets.upload(file, purpose));
    }
}
