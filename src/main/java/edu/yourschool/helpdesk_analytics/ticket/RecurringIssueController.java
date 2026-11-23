package edu.yourschool.helpdesk_analytics.ticket;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecurringIssueController {

    private final TicketRepository tickets;
    private final RecurringIssueDetector detector;

    public RecurringIssueController(TicketRepository tickets, RecurringIssueDetector detector) {
        this.tickets = tickets;
        this.detector = detector;
    }

    @GetMapping("/recurring")
    public String recurring(Model model) {
        model.addAttribute("issues", detector.detect(tickets.findAll()));
        return "tickets/recurring";
    }
}
