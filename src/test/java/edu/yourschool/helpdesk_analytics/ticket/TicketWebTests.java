package edu.yourschool.helpdesk_analytics.ticket;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketWebTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TicketStatusHistoryRepository history;

    @Test
    void rendersFormAndEmptyList() throws Exception {
        mvc.perform(get("/tickets/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"title\"")))
                .andExpect(content().string(containsString("name=\"description\"")))
                .andExpect(content().string(containsString("name=\"priority\"")))
                .andExpect(content().string(containsString("name=\"category\"")));
        mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No tickets yet.")));
    }

    @Test
    void submitsPersistsAndListsTicketWithServerOwnedDefaults() throws Exception {
        mvc.perform(post("/tickets")
                        .param("title", "  Cannot sign in  ")
                        .param("description", "Login fails after the update.")
                        .param("priority", "HIGH")
                        .param("category", "ACCOUNT")
                        .param("id", "999")
                        .param("status", "CLOSED")
                        .param("createdDate", "2000-01-01T00:00:00Z"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        tickets.flush();
        Ticket ticket = tickets.findAll().getFirst();
        assertThat(tickets.count()).isEqualTo(1);
        assertThat(ticket.getId()).isNotNull().isNotEqualTo(999L);
        assertThat(ticket.getTitle()).isEqualTo("Cannot sign in");
        assertThat(ticket.getDescription()).isEqualTo("Login fails after the update.");
        assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.OPEN);
        assertThat(ticket.getPriority()).isEqualTo(Ticket.Priority.HIGH);
        assertThat(ticket.getCategory()).isEqualTo(Ticket.Category.ACCOUNT);
        assertThat(ticket.getCreatedDate()).isNotNull().isAfter(java.time.Instant.parse("2000-01-01T00:00:00Z"));

        mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cannot sign in")))
                .andExpect(content().string(containsString("Login fails after the update.")))
                .andExpect(content().string(containsString("OPEN")))
                .andExpect(content().string(containsString("HIGH")))
                .andExpect(content().string(containsString("ACCOUNT")))
                .andExpect(content().string(containsString(ticket.getCreatedDate().toString())));
    }

    @Test
    void rejectsInvalidInputWithoutSavingAndKeepsFormValues() throws Exception {
        for (String title : new String[] { "   ", "x".repeat(201) }) {
            mvc.perform(post("/tickets").param("title", title).param("description", "Details"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tickets/new"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(content().string(containsString("Details")));
        }
        for (String description : new String[] { "   ", "x".repeat(5001) }) {
            mvc.perform(post("/tickets").param("title", "Issue").param("description", description))
                    .andExpect(view().name("tickets/new"))
                    .andExpect(model().attributeExists("error"));
        }
        mvc.perform(post("/tickets"))
                .andExpect(model().attributeExists("error"));
        assertThat(tickets.count()).isZero();
    }

    @Test
    void rejectsTicketWithoutPriority() throws Exception {
        mvc.perform(post("/tickets")
                        .param("title", "Printer jammed")
                        .param("description", "Paper stuck in tray 2.")
                        .param("category", "HARDWARE"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/new"))
                .andExpect(model().attributeExists("error"));
        assertThat(tickets.count()).isZero();
    }

    @Test
    void listsAllTicketsNewestFirstAndEscapesUserContent() throws Exception {
        tickets.saveAndFlush(new Ticket("Older", "First description",
                Ticket.Priority.LOW, Ticket.Category.OTHER));
        tickets.saveAndFlush(new Ticket("<script>alert(1)</script>", "Second description",
                Ticket.Priority.LOW, Ticket.Category.OTHER));

        String html = mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("Second description")).isLessThan(html.indexOf("First description"));
    }

    @Test
    void changingStatusRecordsHistoryOldestFirst() throws Exception {
        Ticket ticket = tickets.saveAndFlush(new Ticket("VPN drops", "Disconnects every hour.",
                Ticket.Priority.MEDIUM, Ticket.Category.NETWORK));
        String detailUrl = "/tickets/" + ticket.getId();

        mvc.perform(post(detailUrl + "/status").param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl));
        mvc.perform(post(detailUrl + "/status").param("status", "CLOSED"));
        mvc.perform(post(detailUrl + "/status").param("status", "CLOSED"));

        List<TicketStatusHistory> rows = history.findByTicketOrderByIdAsc(ticket);
        assertThat(rows).extracting(TicketStatusHistory::getOldStatus)
                .containsExactly(Ticket.Status.OPEN, Ticket.Status.IN_PROGRESS);
        assertThat(rows).extracting(TicketStatusHistory::getNewStatus)
                .containsExactly(Ticket.Status.IN_PROGRESS, Ticket.Status.CLOSED);
        assertThat(rows).allSatisfy(row -> assertThat(row.getChangedAt()).isNotNull());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(Ticket.Status.CLOSED);

        String html = mvc.perform(get(detailUrl))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("VPN drops", "Disconnects every hour.", "MEDIUM", "NETWORK");
        assertThat(html.indexOf("OPEN → IN_PROGRESS")).isNotNegative()
                .isLessThan(html.indexOf("IN_PROGRESS → CLOSED"));

        mvc.perform(get("/tickets"))
                .andExpect(content().string(containsString("href=\"" + detailUrl + "\"")));
    }

    @Test
    void resolvingRecordsResolvedTimeOnlyTheFirstTime() throws Exception {
        Ticket ticket = tickets.saveAndFlush(new Ticket("Laptop won't boot", "Black screen after logo.",
                Ticket.Priority.HIGH, Ticket.Category.HARDWARE));
        String detailUrl = "/tickets/" + ticket.getId();
        assertThat(ticket.getResolvedDate()).isNull();

        mvc.perform(post(detailUrl + "/status").param("status", "RESOLVED"));
        Instant firstResolved = tickets.findById(ticket.getId()).orElseThrow().getResolvedDate();
        assertThat(firstResolved).isNotNull();

        mvc.perform(get(detailUrl))
                .andExpect(content().string(containsString(firstResolved.toString())));

        mvc.perform(post(detailUrl + "/status").param("status", "OPEN"));
        mvc.perform(post(detailUrl + "/status").param("status", "RESOLVED"));
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getResolvedDate()).isEqualTo(firstResolved);
    }
}
