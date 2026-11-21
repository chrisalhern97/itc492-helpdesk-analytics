package edu.yourschool.helpdesk_analytics.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    List<TicketStatusHistory> findByTicketOrderByIdAsc(Ticket ticket);
}
