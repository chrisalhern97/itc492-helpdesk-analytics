package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class Ticket {

    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    public enum Category { HARDWARE, SOFTWARE, NETWORK, ACCOUNT, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    private Instant resolvedDate;

    protected Ticket() {
        // Required by JPA.
    }

    public Ticket(String title, String description, Priority priority, Category category) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
    }

    Ticket(String title, String description, Priority priority, Category category, Instant createdDate) {
        this(title, description, priority, category);
        this.createdDate = createdDate;
    }

    @PrePersist
    private void recordCreatedDate() {
        if (createdDate == null) {
            createdDate = Instant.now();
        }
    }

    TicketStatusHistory changeStatus(Status newStatus) {
        return changeStatus(newStatus, Instant.now());
    }

    TicketStatusHistory changeStatus(Status newStatus, Instant changedAt) {
        TicketStatusHistory change = new TicketStatusHistory(this, status, newStatus, changedAt);
        status = newStatus;
        if (newStatus == Status.RESOLVED && resolvedDate == null) {
            resolvedDate = changedAt;
        }
        return change;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Category getCategory() { return category; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getResolvedDate() { return resolvedDate; }
}
