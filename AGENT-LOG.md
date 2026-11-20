# AGENT-LOG

Record of AI assistance on this project. Tools used: ChatGPT, Codex (ChatGPT web), Claude (chat), Claude Code (VS Code).

| Date | Issue | Tool | What the agent did | What I checked |
|---|---|---|---|---|
| 2026-09-27 | — | ChatGPT | Drafted initial database tables (tickets, ticket_status_history, problems); notes moved to notes/database-design.md | Reviewed the fields myself against my scope list |
| 2026-09-28 | #4 | Claude (chat) | Walked me through Spring Initializr (Web, Thymeleaf, JPA, H2, DevTools) and running the app in IntelliJ | App started on port 8080 ("Started HelpdeskAnalyticsApplication"); localhost:8080 returned the expected 404 Whitelabel page |
| 2026-09-28 | #5 | Claude (chat) | Guided installing Git, cloning the repo in IntelliJ, merging .gitignore, first commit and push from the IDE | Commit showed 8 added / 1 modified, no .idea or target; files appeared on GitHub; issue #4 closed automatically |
| 2026-09-28 | — | Claude (chat) | Pointed out README said Spring Boot 3 while the project uses 4 | Read pom.xml myself: Spring Boot 4.1.1, Java 21; updated README |
| 2026-10-13 | — | Codex (cloud) | Ran cloud environment setup: installed Java 21 compiler, configured Maven proxy; stopped on a model-capacity error | Not verified|
| 2026-10-15 | — | Claude (chat) | Guided installing VS Code and the Claude Code extension; set it to Manual approval mode | Claude Code confirmed it was connected to this repo on main |
| 2026-10-15 | — | Codex | Created ticket package (Ticket, TicketController, TicketRepository)  | Read Ticket.java, TicketRepository.java and TicketController.java line by line on 2026-11-18; ran the app, submitted a valid ticket (appeared in the list) and an empty-title ticket (error message shown) |
| 2026-11-18 | — | Claude (chat) | Explained Ticket.java, TicketRepository.java and TicketController.java line by line, and pointed out missing fields (priority, category, resolved_at) and the single OPEN status | Compared each explanation against the code myself |
| 2026-11-19 | #10 | Claude Code | Added priority and category to tickets: form dropdowns, list columns, validation, and a test | Test failed before the feature and passes now; all 6 tests pass; ran the app and submitted three tickets with different priorities and categories, all shown correctly in the list |
