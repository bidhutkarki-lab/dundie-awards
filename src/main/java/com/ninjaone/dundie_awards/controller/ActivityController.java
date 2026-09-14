package com.ninjaone.dundie_awards.controller;

import com.ninjaone.dundie_awards.dto.ActivityResponse;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.service.ActivityService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
@Validated
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public PageResponse<ActivityResponse> getActivities(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return activityService.getActivities(page, size);
    }
}
