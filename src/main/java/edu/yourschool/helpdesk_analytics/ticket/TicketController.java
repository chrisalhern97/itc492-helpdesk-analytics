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
        return showForm(model, "", "", "", "");
    }

    @PostMapping
    public String submit(@RequestParam(defaultValue = "") String title,
                         @RequestParam(defaultValue = "") String description,
                         @RequestParam(defaultValue = "") String priority,
                         @RequestParam(defaultValue = "") String category, Model model) {
        title = title.strip();
        description = description.strip();
        Ticket.Priority chosenPriority = parse(Ticket.Priority.class, priority);
        Ticket.Category chosenCategory = parse(Ticket.Category.class, category);
        if (title.isEmpty() || title.length() > 200
                || description.isEmpty() || description.length() > 5000
                || chosenPriority == null || chosenCategory == null) {
            model.addAttribute("error", "Enter a title (1–200 characters) and a description (1–5000 characters), "
                    + "and choose a priority and a category.");
            return showForm(model, title, description, priority, category);
        }
        tickets.save(new Ticket(title, description, chosenPriority, chosenCategory));
        return "redirect:/tickets";
    }

    private String showForm(Model model, String title, String description, String priority, String category) {
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("priority", priority);
        model.addAttribute("category", category);
        model.addAttribute("priorities", Ticket.Priority.values());
        model.addAttribute("categories", Ticket.Category.values());
        return "tickets/new";
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
