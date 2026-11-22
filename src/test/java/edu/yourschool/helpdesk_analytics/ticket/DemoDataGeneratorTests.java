package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DemoDataGeneratorTests {

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TicketStatusHistoryRepository history;

    @Test
    void generatesTicketsWithPlantedRecurringProblem() {
        Instant now = Instant.now();
        DemoDataGenerator generator = new DemoDataGenerator(tickets, history);

        List<Ticket> planted = generator.generate(now);

        assertThat(tickets.count()).isEqualTo(300);
        assertThat(planted).hasSize(25).allSatisfy(ticket -> {
            assertThat(ticket.getId()).isNotNull();
            assertThat(ticket.getCategory()).isEqualTo(Ticket.Category.NETWORK);
        });
        List<Instant> plantedDates = planted.stream().map(Ticket::getCreatedDate).sorted().toList();
        assertThat(Duration.between(plantedDates.getFirst(), plantedDates.getLast()))
                .isLessThanOrEqualTo(Duration.ofDays(14));
        assertThat(tickets.findAll()).allSatisfy(ticket -> assertThat(ticket.getCreatedDate())
                .isAfter(now.minus(Duration.ofDays(90))).isBefore(now));

        List<Ticket> plantedAgain = generator.generate(now);
        assertThat(plantedAgain).extracting(Ticket::getTitle)
                .containsExactlyElementsOf(planted.stream().map(Ticket::getTitle).toList());
    }
}
