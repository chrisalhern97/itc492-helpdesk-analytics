package edu.yourschool.helpdesk_analytics.ticket;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository tickets;

    public TicketController(TicketRepository tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tickets", tickets.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "tickets/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("title", "");
        model.addAttribute("description", "");
        return "tickets/new";
    }

    @PostMapping
    public String submit(@RequestParam(defaultValue = "") String title,
                         @RequestParam(defaultValue = "") String description, Model model) {
        title = title.strip();
        description = description.strip();
        if (title.isEmpty() || title.length() > 200
                || description.isEmpty() || description.length() > 5000) {
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            model.addAttribute("error", "Enter a title (1–200 characters) and a description (1–5000 characters).");
            return "tickets/new";
        }
        tickets.save(new Ticket(title, description));
        return "redirect:/tickets";
    }
}
