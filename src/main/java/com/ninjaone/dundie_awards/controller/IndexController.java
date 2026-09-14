package com.ninjaone.dundie_awards.controller;

import com.ninjaone.dundie_awards.service.ActivityService;
import com.ninjaone.dundie_awards.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeService employeeService;
    private final ActivityService activityService;

    // the two tables page independently, so they cannot share page/size
    @GetMapping
    public String getIndex(
            @RequestParam(defaultValue = "0") int employeePage,
            @RequestParam(defaultValue = "10") int employeeSize,
            @RequestParam(defaultValue = "0") int activityPage,
            @RequestParam(defaultValue = "10") int activitySize,
            Model model) {
        // a hand-edited URL should still render a page, so clamp instead of failing the whole view
        model.addAttribute("employees", employeeService.getEmployees(clampPage(employeePage), clampSize(employeeSize)));
        model.addAttribute(
                "activities", activityService.getActivities(clampPage(activityPage), clampSize(activitySize)));
        return "index";
    }

    private static int clampPage(int page) {
        return Math.max(page, 0);
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
