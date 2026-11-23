package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecurringIssueDetectorTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TicketStatusHistoryRepository history;

    @Test
    void findsThePlantedVpnBurstInTheDemoData() throws Exception {
        List<Ticket> planted = new DemoDataGenerator(tickets, history).generate(Instant.now());
        Set<Long> plantedIds = planted.stream().map(Ticket::getId).collect(Collectors.toSet());

        List<RecurringIssueDetector.RecurringIssue> issues = new RecurringIssueDetector().detect(tickets.findAll());

        assertThat(issues).allSatisfy(issue -> assertThat(issue.tickets())
                .allSatisfy(ticket -> assertThat(ticket.getCategory()).isEqualTo(issue.category())));
        assertThat(issues).anySatisfy(issue -> {
            long plantedFound = issue.tickets().stream().filter(t -> plantedIds.contains(t.getId())).count();
            assertThat(plantedFound).isGreaterThanOrEqualTo(20);
            assertThat(issue.tickets().size() - plantedFound).isLessThanOrEqualTo(3);
        });

        mvc.perform(get("/recurring"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/tickets/" + planted.getFirst().getId() + "\"")));
        mvc.perform(get("/tickets"))
                .andExpect(content().string(containsString("href=\"/recurring\"")));
    }
}
