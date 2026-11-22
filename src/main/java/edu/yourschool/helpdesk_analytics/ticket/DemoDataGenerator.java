package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "helpdesk.demo-data", havingValue = "true")
public class DemoDataGenerator implements ApplicationRunner {

    private static final long SEED = 492;
    private static final int DAYS = 90;
    private static final int ORDINARY_COUNT = 265;
    private static final int ORDINARY_VPN_COUNT = 10;
    private static final int PLANTED_COUNT = 25;
    // The planted burst is created between 45 and 32 days ago.
    private static final int PLANTED_FIRST_DAY_AGO = 45;
    private static final int PLANTED_DAYS = 14;

    private record Template(Ticket.Category category, String title, String description) {}

    private static final List<Template> ORDINARY = List.of(
            new Template(Ticket.Category.HARDWARE, "Printer jammed", "The printer on my floor jams on every job."),
            new Template(Ticket.Category.HARDWARE, "Laptop battery drains fast", "My laptop battery goes from full to empty in about two hours."),
            new Template(Ticket.Category.HARDWARE, "Monitor flickering", "My second monitor flickers and sometimes goes black."),
            new Template(Ticket.Category.HARDWARE, "Keyboard keys not working", "Several keys on my keyboard stopped responding."),
            new Template(Ticket.Category.HARDWARE, "Docking station not detected", "My laptop does not detect the docking station or the external screens."),
            new Template(Ticket.Category.SOFTWARE, "Excel crashes when opening files", "Excel closes without warning when I open large spreadsheets."),
            new Template(Ticket.Category.SOFTWARE, "Cannot install software", "I need a program for my project but the installer asks for admin rights."),
            new Template(Ticket.Category.SOFTWARE, "Outlook not syncing", "New emails do not show up in Outlook until I restart it."),
            new Template(Ticket.Category.SOFTWARE, "Teams microphone not working", "Nobody can hear me in Teams meetings, although the microphone works elsewhere."),
            new Template(Ticket.Category.SOFTWARE, "PDF files will not open", "Double-clicking a PDF shows an error instead of opening it."),
            new Template(Ticket.Category.NETWORK, "Wi-Fi slow in meeting room", "The wireless connection in the large meeting room is very slow."),
            new Template(Ticket.Category.NETWORK, "No network on desk port", "The network port at my desk does not give me a connection."),
            new Template(Ticket.Category.NETWORK, "Shared drive not reachable", "I cannot open the shared department drive."),
            new Template(Ticket.Category.NETWORK, "Website blocked by mistake", "A website I need for work is being blocked."),
            new Template(Ticket.Category.ACCOUNT, "Password reset", "I forgot my password and cannot sign in."),
            new Template(Ticket.Category.ACCOUNT, "Account locked", "My account was locked after too many sign-in attempts."),
            new Template(Ticket.Category.ACCOUNT, "New employee account", "A new team member starts on Monday and needs an account."),
            new Template(Ticket.Category.ACCOUNT, "Access to shared folder", "I need permission to open a folder my team uses."),
            new Template(Ticket.Category.ACCOUNT, "Sign-in approval not arriving", "The sign-in approval request does not arrive on my phone."),
            new Template(Ticket.Category.OTHER, "Replacement mouse", "The scroll wheel on my mouse is broken. Could I get a new one?"),
            new Template(Ticket.Category.OTHER, "Question about a licence", "Do we have a licence for a diagram tool I could use?"),
            new Template(Ticket.Category.OTHER, "Desk phone needs moving", "My desk phone needs to be set up at my new desk."),
            new Template(Ticket.Category.OTHER, "Training request", "Is there any training available for the new expense system?"));

    private static final List<Template> ORDINARY_VPN = List.of(
            new Template(Ticket.Category.NETWORK, "Install VPN on new laptop", "I received a new laptop and need the VPN client installed."),
            new Template(Ticket.Category.NETWORK, "Reset VPN password", "I forgot my VPN password and cannot connect from home."),
            new Template(Ticket.Category.NETWORK, "VPN access for contractor", "A contractor on our team needs VPN access for the next three months."),
            new Template(Ticket.Category.NETWORK, "VPN certificate warning", "The VPN client says my certificate expires soon. What should I do?"),
            new Template(Ticket.Category.NETWORK, "Using the VPN while travelling", "I am travelling next week. How do I connect to the VPN from a hotel?"));

    private static final List<String> PLANTED_TITLES = List.of(
            "VPN keeps disconnecting",
            "VPN drops after update",
            "Remote connection drops every few minutes",
            "Can't stay connected to VPN",
            "Lost VPN connection again",
            "Disconnected from work network since update",
            "vpn disconnects randomly",
            "Remote access unstable after last update");

    private static final List<String> PLANTED_DESCRIPTIONS = List.of(
            "Since the update installed this week, the VPN disconnects every 10 to 15 minutes and I have to reconnect.",
            "After my laptop restarted for updates, remote access keeps dropping during calls.",
            "The VPN client shows connected, then drops a few minutes later. It started after the latest update.",
            "When I connect from home I get kicked off within half an hour. Colleagues say the same since the update.",
            "The connection to the office network keeps cutting out. It was fine before the update on Tuesday.",
            "vpn drops constantly, have to log in again each time. the update was pushed yesterday");

    private static final List<String> EXTRA_DETAILS = List.of(
            "",
            " It started this morning.",
            " This is blocking my work.",
            " It has happened a few times this week.",
            " A colleague has the same problem.",
            " I already tried restarting.");

    private final TicketRepository tickets;
    private final TicketStatusHistoryRepository history;

    public DemoDataGenerator(TicketRepository tickets, TicketStatusHistoryRepository history) {
        this.tickets = tickets;
        this.history = history;
    }

    @Override
    public void run(ApplicationArguments args) {
        generate(Instant.now());
    }

    List<Ticket> generate(Instant now) {
        Random random = new Random(SEED);
        Instant today = now.truncatedTo(ChronoUnit.DAYS);
        List<Ticket> all = new ArrayList<>();

        for (int i = 0; i < ORDINARY_COUNT; i++) {
            Template template = pick(ORDINARY, random);
            Instant created = workingTime(today, 1 + random.nextInt(DAYS - 1), random);
            all.add(fromTemplate(template, randomPriority(random), created, random));
        }
        for (int i = 0; i < ORDINARY_VPN_COUNT; i++) {
            Template template = pick(ORDINARY_VPN, random);
            Instant created = workingTime(today, 1 + i * 9 + random.nextInt(8), random);
            all.add(fromTemplate(template, randomPriority(random), created, random));
        }
        List<Ticket> planted = new ArrayList<>();
        for (int i = 0; i < PLANTED_COUNT; i++) {
            String title = pick(PLANTED_TITLES, random);
            String description = pick(PLANTED_DESCRIPTIONS, random);
            Ticket.Priority priority = random.nextBoolean() ? Ticket.Priority.HIGH : Ticket.Priority.MEDIUM;
            Instant created = workingTime(today, PLANTED_FIRST_DAY_AGO - random.nextInt(PLANTED_DAYS), random);
            planted.add(new Ticket(title, description, priority, Ticket.Category.NETWORK, created));
        }
        all.addAll(planted);

        all.sort(Comparator.comparing(Ticket::getCreatedDate));
        for (Ticket ticket : all) {
            List<TicketStatusHistory> changes = workOn(ticket, now, random);
            tickets.save(ticket);
            history.saveAll(changes);
        }
        return planted;
    }

    private static Ticket fromTemplate(Template template, Ticket.Priority priority, Instant created, Random random) {
        String description = template.description() + pick(EXTRA_DETAILS, random);
        return new Ticket(template.title(), description, priority, template.category(), created);
    }

    // Every random value is drawn before checking it against now, so the time of day
    // the app starts cannot change the sequence of random numbers.
    private static List<TicketStatusHistory> workOn(Ticket ticket, Instant now, Random random) {
        boolean neverPickedUp = random.nextInt(100) < 5;
        Instant started = ticket.getCreatedDate().plus(Duration.ofMinutes(10 + random.nextInt(230)));
        Instant resolved = started.plus(timeToResolve(ticket.getPriority(), random));
        Instant closed = resolved.plus(Duration.ofHours(24 + random.nextInt(48)));
        boolean leftResolved = random.nextInt(100) < 20;

        List<TicketStatusHistory> changes = new ArrayList<>();
        if (neverPickedUp || started.isAfter(now)) {
            return changes;
        }
        changes.add(ticket.changeStatus(Ticket.Status.IN_PROGRESS, started));
        if (resolved.isAfter(now)) {
            return changes;
        }
        changes.add(ticket.changeStatus(Ticket.Status.RESOLVED, resolved));
        if (leftResolved || closed.isAfter(now)) {
            return changes;
        }
        changes.add(ticket.changeStatus(Ticket.Status.CLOSED, closed));
        return changes;
    }

    private static Duration timeToResolve(Ticket.Priority priority, Random random) {
        return switch (priority) {
            case CRITICAL -> Duration.ofMinutes(60 + random.nextInt(7 * 60));
            case HIGH -> Duration.ofMinutes(4 * 60 + random.nextInt(20 * 60));
            case MEDIUM -> Duration.ofMinutes(8 * 60 + random.nextInt(64 * 60));
            case LOW -> Duration.ofMinutes(24 * 60 + random.nextInt(6 * 24 * 60));
        };
    }

    private static Ticket.Priority randomPriority(Random random) {
        int roll = random.nextInt(100);
        if (roll < 30) {
            return Ticket.Priority.LOW;
        }
        if (roll < 70) {
            return Ticket.Priority.MEDIUM;
        }
        if (roll < 92) {
            return Ticket.Priority.HIGH;
        }
        return Ticket.Priority.CRITICAL;
    }

    private static Instant workingTime(Instant today, int daysAgo, Random random) {
        return today.minus(Duration.ofDays(daysAgo)).plus(Duration.ofMinutes(8 * 60 + random.nextInt(10 * 60)));
    }

    private static <T> T pick(List<T> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }
}
