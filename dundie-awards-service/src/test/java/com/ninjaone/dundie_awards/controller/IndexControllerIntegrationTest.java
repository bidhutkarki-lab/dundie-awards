package com.ninjaone.dundie_awards.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import com.ninjaone.dundie_awards.SynchronousActivityConfiguration;
import com.ninjaone.dundie_awards.TestcontainersConfiguration;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.ActivityRepository;
import com.ninjaone.dundie_awards.repository.DundieAwardRepository;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SynchronousActivityConfiguration.class})
class IndexControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DundieAwardRepository dundieAwardRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void resetData() {
        dundieAwardRepository.deleteAll();
        activityRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void indexRendersEmployeesAndActivities() throws Exception {
        Organization organization = organizationRepository.save(new Organization("Dunder Mifflin"));
        employeeRepository.save(new Employee("Michael", "Scott", organization));
        activityRepository.save(new Activity(LocalDateTime.of(2024, 1, 1, 10, 0), "employee.created id=1"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(Matchers.containsString("Michael")))
                .andExpect(content().string(Matchers.containsString("Dunder Mifflin")))
                .andExpect(content().string(Matchers.containsString("employee.created id=1")))
                .andExpect(content().string(Matchers.containsString("2024-01-01T10:00")));
    }

    @Test
    void indexShowsEmptyStateWhenNoActivitiesExist() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("No activities")));
    }

    @Test
    void indexRendersActivitiesNewestFirst() throws Exception {
        saveActivities(3);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.stringContainsInOrder("event-3", "event-2", "event-1")));
    }

    @Test
    void indexPaginatesActivitiesWithDefaultSizeOfTen() throws Exception {
        saveActivities(12);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Page 1 of 2")))
                .andExpect(content().string(Matchers.containsString("12 activities total")))
                .andExpect(content().string(Matchers.containsString("event-12")))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">event-2<"))));
    }

    @Test
    void indexRendersRequestedActivityPage() throws Exception {
        saveActivities(12);

        mockMvc.perform(get("/").param("activityPage", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Page 2 of 2")))
                .andExpect(content().string(Matchers.containsString(">event-2<")))
                .andExpect(content().string(Matchers.containsString(">event-1<")))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">event-12<"))));
    }

    @Test
    void indexPaginatesEmployees() throws Exception {
        saveEmployees(12);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("12 employees total")))
                .andExpect(content().string(Matchers.containsString(">First1<")))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">First11<"))));

        mockMvc.perform(get("/").param("employeePage", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(">First11<")))
                .andExpect(content().string(Matchers.containsString(">First12<")))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">First1<"))));
    }

    @Test
    void indexPagesEmployeesAndActivitiesIndependently() throws Exception {
        saveEmployees(12);
        saveActivities(12);

        // paging employees must not reset the activity log back to its first page
        mockMvc.perform(get("/").param("employeePage", "1").param("activityPage", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(">First11<")))
                .andExpect(content().string(Matchers.containsString(">event-2<")))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">First1<"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString(">event-12<"))));
    }

    @Test
    void indexPaginationLinksPreserveTheOtherTablesPage() throws Exception {
        saveEmployees(12);
        saveActivities(12);

        mockMvc.perform(get("/").param("activityPage", "1"))
                .andExpect(status().isOk())
                // the employee "Next" link must carry the activity log's current page
                .andExpect(content().string(Matchers.containsString("employeePage=1")))
                .andExpect(content().string(Matchers.containsString("activityPage=1")));
    }

    @Test
    void indexRendersPaginationLinksCarryingPageAndSize() throws Exception {
        saveActivities(12);

        mockMvc.perform(get("/").param("activityPage", "0").param("activitySize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("activityPage=1&amp;activitySize=5")));
    }

    @Test
    void indexNeverLinksToAnOutOfRangePage() throws Exception {
        saveActivities(12);

        mockMvc.perform(get("/").param("activityPage", "0").param("activitySize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("activityPage=-1"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("employeePage=-1"))));

        mockMvc.perform(get("/").param("activityPage", "2").param("activitySize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Page 3 of 3")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("activityPage=3"))));
    }

    @Test
    void indexClampsOutOfRangeParamsInsteadOfFailing() throws Exception {
        saveActivities(3);

        mockMvc.perform(get("/").param("activityPage", "-5").param("activitySize", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("event-3")));

        mockMvc.perform(get("/").param("activitySize", "9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("event-3")));

        mockMvc.perform(get("/").param("employeePage", "-5").param("employeeSize", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("event-3")));
    }

    private void saveEmployees(int count) {
        Organization organization = organizationRepository.save(new Organization("Dunder Mifflin"));
        IntStream.rangeClosed(1, count)
                .forEach(i -> employeeRepository.save(new Employee("First" + i, "Last" + i, organization)));
    }

    private void saveActivities(int count) {
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 10, 0);
        IntStream.rangeClosed(1, count)
                .forEach(i -> activityRepository.save(new Activity(base.plusMinutes(i), "event-" + i)));
    }
}
