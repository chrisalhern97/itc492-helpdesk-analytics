# ITC 492 Senior Design — Detecting Recurring IT Problems Through Help Desk Ticket Analytics

**Author:** Christopher Hernandez
**Advisor:** Professor Hollister
**Term:** Fall 2026

---

## Problem

IT help desks handle hundreds of support requests, and many of them are really the
same underlying issue showing up again and again, like a bad software update that
causes dozens of separate login complaints. Staff usually handle each request one at
a time, so these repeating causes are easy to miss. The organization keeps fixing
symptoms instead of the real problem, and it has little visibility into whether
requests are being handled on time.

## Proposed Solution

A lightweight help desk ticketing system paired with an analytics layer. The ticketing
side records support requests and their history. The analytics side measures how well
requests are being handled (for example, how many meet their time targets) and
automatically groups similar tickets by their descriptions to flag likely recurring
problems. The system is tested on realistic synthetic data with deliberately planted
root causes, to check whether the analysis can find them on its own.

## Scope

**In scope:**
- [ ] Basic ticket handling: create, categorize, prioritize, assign, resolve/close
- [ ] Simple change history recording every ticket status change and when it happened
- [ ] SLA compliance calculated from fixed response/resolution targets per priority
- [ ] Service metrics dashboard: SLA compliance, mean time to resolve (MTTR),
      ticket aging, volume trends
- [ ] Recurring-issue detection by clustering similar ticket descriptions
- [ ] Problem records that link multiple related tickets to one underlying cause
- [ ] Synthetic data generator with documented methodology and planted root causes
- [ ] Evaluation: does the analysis surface the planted causes without being told?
- [ ] Public deployment to an Oracle Cloud free-tier VPS, reachable at a public URL
      (course requirement: the app must be publicly hosted before term end)

**Out of scope:**
- User accounts, logins, and role-based access (requester / technician / manager).
  The system runs as a single view.
- Business-hours SLA clocks and pausing the clock while waiting on a customer
- Reopening closed tickets as a workflow feature
- Asset / configuration item (CI) records
- First-contact resolution metric
- Email intake, notifications, file attachments, and a customer-facing portal
- Use with real users or real ticket data. The deployed instance runs on
  synthetic data only.

*These are candidates for future work, not part of this semester's deliverable.*

## Tech Stack

| Layer | Choice                                                                                                                     | Why |
|---|----------------------------------------------------------------------------------------------------------------------------|---|
| Frontend | Thymeleaf templates + Bootstrap                                                                                            | Integrates directly with Spring Boot; Bootstrap gives a clean look without hand-written CSS |
| Backend | Java 21 + Spring Boot 4                                                                                                    | Primary language; Spring Boot's built-in CRUD scaffolding keeps the ticketing half deliberately small |
| Database | H2 (local development), PostgreSQL (deployed)                                                                              | H2 needs no setup locally; PostgreSQL is the durable choice on the server. Spring Data JPA makes the switch a configuration change |
| Hosting / Deploy | Oracle Cloud Free Tier VPS (Ubuntu), app run as a systemd service                                                          | Course requires public hosting before term end; Oracle's always-free tier covers it at no cost |
| Analytics / Reporting | SQL aggregate queries (service metrics), TF-IDF word weights + cosine similarity in plain Java (recurring-issue grouping), Chart.js (dashboard charts) | SQL handles MTTR / SLA / aging / volume directly; TF-IDF and cosine similarity are standard, citable text-mining techniques that need no extra library and can group by wording, category and time together without choosing a number of clusters in advance |

## Repository Layout

```
/docs          Proposal, report drafts, presentation slides
/src           Application source
/data          Sample and seed data (NO real or sensitive data — see note below)
/notes         Literature notes, meeting notes, design sketches
AGENT-LOG.md   Record of AI assistance used on this project
```

## Running It Locally

*Fill this in as soon as there's something to run. Write it so that a grader who
has never seen your project can get it running in under ten minutes.*

```bash
# setup steps go here
```

## Demo Data

The database starts empty. To fill it with repeatable synthetic tickets, start the app
with the `helpdesk.demo-data` setting turned on. It is off by default.

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--helpdesk.demo-data=true"
```

You can also add `helpdesk.demo-data=true` to `src/main/resources/application.properties`
(do not commit that change).

**What gets generated** (`DemoDataGenerator` in the `ticket` package):

- 300 tickets created during working hours over the 90 days before the app started,
  with a mix of priorities, categories and statuses.
- Each ticket moves through OPEN → IN_PROGRESS → RESOLVED → CLOSED at realistic
  times, with a status history row for every change. Higher priorities are resolved
  faster. Recent tickets are often still OPEN or IN_PROGRESS.
- The generator uses a fixed random seed (492), so the tickets, their wording and
  their timing are the same every run. Dates are relative to the moment the app
  starts, so the calendar dates move with the day you run it.

**Planted recurring problem:** 25 of the tickets report VPN / remote-access
disconnects after a software update. They are all in category NETWORK, worded in
several different ways (not all of them use the word "VPN"), and all created within
the same two weeks, 32 to 45 days before startup. Ten ordinary VPN tickets (installs,
password resets, access requests) are spread across the full 90 days, so the planted
problem cannot be found just by searching for "VPN". Nothing in the database marks
which tickets were planted.

The demo data is loaded on every start. That is fine with H2, which starts empty;
against a persistent database such as PostgreSQL it would add another 300 tickets
each time.

## Deployment

The course requires this application to be publicly hosted before the end of term,
not only runnable locally. Target: an Oracle Cloud Free Tier VPS.

*Fill in the public URL and deployment steps once it is live.*

- [ ] Oracle Cloud free-tier account created
- [ ] VPS instance provisioned, SSH access working
- [ ] Java and PostgreSQL installed on the server
- [ ] Firewall / security list opened for the app port
- [ ] App running as a service that survives disconnect and reboot
- [ ] Public URL recorded here

## Status

Current: Scope narrowed to a lightweight ticketing core plus service and
problem-management analytics; public deployment added to scope per course
requirement; proposal in progress.

## A Note on Data

This project uses synthetic/sample data only. No real user data, no real ticket
contents, no credentials are committed to this repository.


