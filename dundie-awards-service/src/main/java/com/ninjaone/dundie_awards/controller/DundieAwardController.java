package com.ninjaone.dundie_awards.controller;

import com.ninjaone.dundie_awards.dto.DundieAwardRequest;
import com.ninjaone.dundie_awards.dto.DundieAwardResponse;
import com.ninjaone.dundie_awards.dto.LeaderboardEntry;
import com.ninjaone.dundie_awards.dto.PageResponse;
import com.ninjaone.dundie_awards.service.DundieAwardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dundie-awards")
@RequiredArgsConstructor
@Validated
public class DundieAwardController {

    private final DundieAwardService dundieAwardService;

    @GetMapping
    public PageResponse<DundieAwardResponse> getAwards(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return dundieAwardService.getAwards(page, size);
    }

    @GetMapping("/leaderboard")
    public PageResponse<LeaderboardEntry> getLeaderboard(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return dundieAwardService.getLeaderboard(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DundieAwardResponse> getAwardById(@PathVariable Long id) {
        return ResponseEntity.ok(dundieAwardService.getAward(id));
    }

    @PostMapping
    public ResponseEntity<DundieAwardResponse> giveAward(@Valid @RequestBody DundieAwardRequest request) {
        return ResponseEntity.ok(dundieAwardService.giveAward(request));
    }
}
