package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RecurringIssueDetector {

    // Chosen once, before running the detection test.
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final Duration WINDOW = Duration.ofDays(14);
    private static final int MIN_TICKETS = 5;
    private static final int MIN_WORD_LENGTH = 3;
    private static final int TOP_WORDS = 5;

    public record RecurringIssue(Ticket.Category category, List<Ticket> tickets,
                                 Instant firstCreated, Instant lastCreated, List<String> topWords) {}

    public List<RecurringIssue> detect(List<Ticket> tickets) {
        List<Map<String, Integer>> counts = tickets.stream().map(RecurringIssueDetector::wordCounts).toList();
        Map<String, Double> idf = inverseDocumentFrequency(counts);
        List<Map<String, Double>> vectors = counts.stream().map(c -> weigh(c, idf)).toList();

        // Link similar tickets in the same category created close together.
        // Each ticket points towards a group leader; linking two tickets merges their groups.
        int[] leader = new int[tickets.size()];
        for (int i = 0; i < leader.length; i++) {
            leader[i] = i;
        }
        for (int i = 0; i < tickets.size(); i++) {
            for (int j = i + 1; j < tickets.size(); j++) {
                Ticket a = tickets.get(i);
                Ticket b = tickets.get(j);
                if (a.getCategory() == b.getCategory()
                        && Duration.between(a.getCreatedDate(), b.getCreatedDate()).abs().compareTo(WINDOW) <= 0
                        && cosine(vectors.get(i), vectors.get(j)) >= SIMILARITY_THRESHOLD) {
                    leader[root(leader, i)] = root(leader, j);
                }
            }
        }
        Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < tickets.size(); i++) {
            groups.computeIfAbsent(root(leader, i), k -> new ArrayList<>()).add(i);
        }

        List<RecurringIssue> issues = new ArrayList<>();
        for (List<Integer> members : groups.values()) {
            List<Ticket> group = members.stream().map(tickets::get)
                    .sorted(Comparator.comparing(Ticket::getCreatedDate)).toList();
            if (mostWithinWindow(group) >= MIN_TICKETS) {
                issues.add(new RecurringIssue(group.getFirst().getCategory(), group,
                        group.getFirst().getCreatedDate(), group.getLast().getCreatedDate(),
                        topWords(members, vectors)));
            }
        }
        issues.sort(Comparator.comparing((RecurringIssue issue) -> issue.tickets().size()).reversed());
        return issues;
    }

    private static Map<String, Integer> wordCounts(Ticket ticket) {
        Map<String, Integer> counts = new HashMap<>();
        String text = (ticket.getTitle() + " " + ticket.getDescription()).toLowerCase(Locale.ROOT);
        for (String word : text.split("[^a-z0-9]+")) {
            if (word.length() >= MIN_WORD_LENGTH) {
                counts.merge(word, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Map<String, Double> inverseDocumentFrequency(List<Map<String, Integer>> counts) {
        Map<String, Integer> ticketsWithWord = new HashMap<>();
        for (Map<String, Integer> ticketCounts : counts) {
            for (String word : ticketCounts.keySet()) {
                ticketsWithWord.merge(word, 1, Integer::sum);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        ticketsWithWord.forEach((word, n) -> idf.put(word, Math.log((double) counts.size() / n)));
        return idf;
    }

    private static Map<String, Double> weigh(Map<String, Integer> counts, Map<String, Double> idf) {
        Map<String, Double> weights = new HashMap<>();
        counts.forEach((word, count) -> weights.put(word, count * idf.get(word)));
        return weights;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            dot += entry.getValue() * b.getOrDefault(entry.getKey(), 0.0);
        }
        double lengths = length(a) * length(b);
        return lengths == 0 ? 0 : dot / lengths;
    }

    private static double length(Map<String, Double> weights) {
        double sum = 0;
        for (double weight : weights.values()) {
            sum += weight * weight;
        }
        return Math.sqrt(sum);
    }

    private static int root(int[] leader, int i) {
        while (leader[i] != i) {
            i = leader[i];
        }
        return i;
    }

    private static int mostWithinWindow(List<Ticket> sortedByCreated) {
        int most = 0;
        int start = 0;
        for (int end = 0; end < sortedByCreated.size(); end++) {
            while (Duration.between(sortedByCreated.get(start).getCreatedDate(),
                    sortedByCreated.get(end).getCreatedDate()).compareTo(WINDOW) > 0) {
                start++;
            }
            most = Math.max(most, end - start + 1);
        }
        return most;
    }

    private static List<String> topWords(List<Integer> members, List<Map<String, Double>> vectors) {
        Map<String, Double> totals = new HashMap<>();
        for (int i : members) {
            vectors.get(i).forEach((word, weight) -> totals.merge(word, weight, Double::sum));
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_WORDS)
                .map(Map.Entry::getKey)
                .toList();
    }
}
