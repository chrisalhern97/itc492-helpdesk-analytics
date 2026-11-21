package edu.yourschool.helpdesk_analytics.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TicketStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Ticket.Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Ticket.Status newStatus;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    protected TicketStatusHistory() {
        // Required by JPA.
    }

    TicketStatusHistory(Ticket ticket, Ticket.Status oldStatus, Ticket.Status newStatus, Instant changedAt) {
        this.ticket = ticket;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public Ticket.Status getOldStatus() { return oldStatus; }
    public Ticket.Status getNewStatus() { return newStatus; }
    public Instant getChangedAt() { return changedAt; }
}
