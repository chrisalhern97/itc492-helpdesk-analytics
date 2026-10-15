package edu.yourschool.helpdesk_analytics.ticket;

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

    @Test
    void rendersFormAndEmptyList() throws Exception {
        mvc.perform(get("/tickets/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"title\"")))
                .andExpect(content().string(containsString("name=\"description\"")));
        mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No tickets yet.")));
    }

    @Test
    void submitsPersistsAndListsTicketWithServerOwnedDefaults() throws Exception {
        mvc.perform(post("/tickets")
                        .param("title", "  Cannot sign in  ")
                        .param("description", "Login fails after the update.")
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
        assertThat(ticket.getCreatedDate()).isNotNull().isAfter(java.time.Instant.parse("2000-01-01T00:00:00Z"));

        mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cannot sign in")))
                .andExpect(content().string(containsString("Login fails after the update.")))
                .andExpect(content().string(containsString("OPEN")))
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
    void listsAllTicketsNewestFirstAndEscapesUserContent() throws Exception {
        tickets.saveAndFlush(new Ticket("Older", "First description"));
        tickets.saveAndFlush(new Ticket("<script>alert(1)</script>", "Second description"));

        String html = mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("Second description")).isLessThan(html.indexOf("First description"));
    }
}
