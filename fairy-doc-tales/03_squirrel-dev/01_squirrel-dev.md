# The Developer Who Was Always Done

Sprint 14 has the highest velocity in FinTrack history. Forty-three story points. The burndown chart touches zero on Wednesday afternoon — two and a half days before the sprint ends.

Mirror Mike sends a congratulatory message to the engineering Slack channel: *"Incredible sprint everyone — 43 points! This is what high-performance delivery looks like. 🚀"*

Bugfinder Betty has been sick all week. She comes back on Thursday.

> Prequels
> - [The Team](../00_prequels/03_create-business-heroes.md)
> - [The Risks](../00_prequels/04_create-business-villains.md)

---

## Scene: The risks hiding in Sprint 14

Before the sprint begins, three business risks are already active at FinTrack. They have no owner, no mitigation, and no place on any risk register.

> **Risk** Create risk
>
> | id | name                    | severity | mitigation                      |
> |----|-------------------------|----------|---------------------------------|
> | 11 | Missing Acceptance Test | HIGH     | Automated Verification          |
> | 12 | Blame Culture           | EXTREME  | Shared Accountability           |
> | 15 | Partial Implementation  | HIGH     | Complete Specification Coverage |

---

## Scene: Sprint 14 — the most important sprint in FinTrack history

Ka-Ching Corp is FinTrack's largest enterprise prospect. After three months of evaluation and two product demos, they have agreed to a pilot. The condition: User Registration must be live and fully working by end of sprint — this Friday.

The User Registration feature is more complex than it sounds. It is not just a form and a database row. It includes email validation, a confirmation email flow, duplicate account prevention, a GDPR-compliant audit log, and a password reset flow. Ka-Ching Corp specifically asked about the audit log — their legal team reviewed the requirement list and highlighted it.

Sprint planning is on Monday morning. Mirror Mike attends for the first five minutes to reinforce the stakes. He leaves to take a call.

> **Sprint** Plan sprint
>
> | id | name      | plannedPoints | goal                                            |
> |----|-----------|---------------|-------------------------------------------------|
> | 1  | Sprint 14 | 43            | User Registration ready for Ka-Ching Corp go-live |

Six tickets are added to the sprint. Together they cover the full User Registration feature. Forty-three story points — an ambitious but achievable target for the team.

> **Sprint** Add task *Sprint 14*
>
> | task                         | points |
> |------------------------------|--------|
> | User Registration            | 13     |
> | Email Validation             | 5      |
> | Confirmation Email           | 5      |
> | Duplicate Registration Check | 5      |
> | GDPR Audit Log               | 8      |
> | Password Reset Flow          | 7      |

Checklist Charlie is assigned User Registration and Email Validation. Rookie Ron takes Confirmation Email and Duplicate Registration Check. Blueprint Ben is responsible for GDPR Audit Log and Password Reset Flow — but he is pulled into an architecture workshop that runs until Wednesday. He hands his tickets to Checklist Charlie on Tuesday morning.

*Six tickets. One developer. Two and a half days. Forty-three points.*

---

## Scene: Before the first line of code — the specification gap

The sprint has started. The tickets have been assigned. The team is ready to build.

There is a question that nobody has asked yet.

What does *done* actually look like for each of these tasks? Not the task title — everyone knows the title. What does a passing outcome look like? What is the specific, concrete, testable example of correct behaviour?

> **Specification** Has no examples
>
> | feature                      |
> |------------------------------|
> | User Registration            |
> | Email Validation             |
> | Confirmation Email           |
> | Duplicate Registration Check |
> | GDPR Audit Log               |

Not one of the five core tasks has a single concrete example. The tickets have descriptions. They have acceptance criteria listed in sentence form. But there is no table that says: given *this* input, the system produces *this* output, and we can verify it.

The GDPR Audit Log ticket reads: *"All registration events must be written to the audit log with timestamp and user identifier."* That sounds clear. But what exactly is a registration event? Is a failed attempt logged? What format is the timestamp? What constitutes the user identifier before the account is confirmed?

Nobody has answered those questions. The developers will answer them themselves — in code, implicitly, at the moment they write each line.

> **Risk** Risk is active
>
> | name                    |
> |-------------------------|
> | Missing Acceptance Test |
> | Partial Implementation  |

The risk is already present. The sprint has not produced a single line of output yet, and the conditions for failure are already in place.

---

## Scene: Monday morning — Checklist Charlie opens his first ticket

9:15 AM. Checklist Charlie opens the User Registration ticket. He has five tickets to complete — six if you count the GDPR Audit Log that Blueprint Ben handed him this morning with an apology and a coffee.

He reads the first acceptance criterion: *"A new user can register with a valid email address and password."*

He nods. He understands this. He opens his IDE.

By 11:30 AM, he has a `POST /api/register` endpoint. It accepts an email and a password. It creates a user record in the database. It returns a 201. He writes three unit tests — happy path, missing email, missing password. All green.

He opens Jira. He clicks the ticket to IN_REVIEW. He writes in the comments: *"Implementation complete. POST /register working. Tests passing."*

He does not look at acceptance criteria two, three, four, or five. He has read criterion one. He has built criterion one. Criterion one is done. The ticket will be fully done when he reviews it — which he will do right now, because he is also the reviewer.

He clicks DONE.

It is 11:47 AM on Monday. The User Registration ticket is green.

> **Sprint** Mark task done
>
> | task              |
> |-------------------|
> | User Registration |

---

## Scene: Tuesday and Wednesday — the domino effect

Checklist Charlie applies the same process to the remaining tickets.

Email Validation: criterion one is *"The system validates that the email address is in the correct format before creating the account."* He adds a regex check to the registration endpoint. Green. Done.

Confirmation Email: criterion one is *"A confirmation email is sent to the user after registration."* He adds a method call to the notification service. It compiles. He does not check whether the notification service actually sends anything in the current environment. Done.

Duplicate Registration Check: criterion one is *"The system rejects registration attempts with an already-registered email address."* He adds a `findByEmail` check. Green. Done.

GDPR Audit Log: criterion one is *"All registration events are written to the audit log."* He adds a single `log.info("Registration event: " + email)` call. A log statement. In the application log. Not the audit log. But it is a log of a registration event. Done.

Password Reset Flow: criterion one is *"A user can request a password reset by providing their registered email address."* He adds a `POST /api/password-reset` endpoint that returns a 200. The email is not sent. The reset token is not generated. The endpoint exists. Done.

By Wednesday afternoon at 3:47 PM, all six tickets are green.

> **Sprint** Mark task done
>
> | task                         |
> |------------------------------|
> | Email Validation             |
> | Confirmation Email           |
> | Duplicate Registration Check |
> | GDPR Audit Log               |
> | Password Reset Flow          |

> **Sprint** Reported velocity is
>
> | sprint    | expected |
> |-----------|----------|
> | Sprint 14 | 43       |

Mirror Mike sees the burndown chart. He posts his congratulatory message.

---

## Scene: Blueprint Ben reviews the pull requests

Wednesday afternoon. Blueprint Ben finishes his architecture workshop and opens the pull requests. There are six of them. Checklist Charlie has been thorough about creating them.

Ben reviews each one in order. The code is clean. The naming conventions are followed. The exception handling is consistent. The dependency injection pattern is correct. He leaves a few minor comments about logging format and one suggestion about error codes.

He approves all six pull requests.

He does not open the ticket alongside the pull request. He does not read the acceptance criteria. He does not count how many of the five criteria are covered by the diff. He is reviewing code quality, not specification coverage.

This is how Blueprint Ben has always done code review. This is how most tech leads do code review. The code is fine. That is not the problem.

> **Sprint** Reported velocity is
>
> | sprint    | expected |
> |-----------|----------|
> | Sprint 14 | 43       |

The sprint board shows six green tickets. Forty-three story points. The most productive sprint in FinTrack history. Pinky Princess books the Ka-Ching Corp demo for Friday 3 PM.

---

## Scene: Thursday — Bugfinder Betty reads every word

Betty returns from sick leave at 8:03 AM. She opens her laptop. She sees Mirror Mike's congratulatory Slack message from the day before. She sees the sprint board: six green tickets.

She opens the User Registration ticket. She reads all five acceptance criteria — not just the first one. She opens the staging environment. She starts testing.

By 9:30 AM, she has found the gaps:

| Task                         | Criteria | Implemented | What is missing                                                    |
|------------------------------|----------|-------------|--------------------------------------------------------------------|
| User Registration            | 5        | 1           | Email validation on input, email confirmation flow, duplicate check, GDPR log |
| Email Validation             | 3        | 0           | The regex is on the registration endpoint, not a dedicated validation service |
| Confirmation Email           | 2        | 0           | `notificationService.send()` is called but the service is not configured  |
| Duplicate Registration Check | 2        | 0           | `findByEmail` returns an error, but the same email registers successfully in tests |
| GDPR Audit Log               | 3        | 0           | `log.info` writes to the application log — the audit log table has zero entries |
| Password Reset Flow          | 4        | 0           | The endpoint returns 200 — no token generated, no email sent      |

She opens each ticket. She changes the status to IN_REVIEW. She writes detailed comments. She tags Checklist Charlie, Blueprint Ben, and Mirror Mike.

Then she calls Mirror Mike directly.

> **Sprint** Task is not verified
>
> | task                         |
> |------------------------------|
> | User Registration            |
> | Email Validation             |
> | Confirmation Email           |
> | Duplicate Registration Check |
> | GDPR Audit Log               |
> | Password Reset Flow          |

*"The sprint is not done,"* she says. *"None of the tickets are fully implemented. The GDPR audit log is a log statement. Ka-Ching Corp's legal team will ask to see audit entries. There are none."*

Mirror Mike is quiet for four seconds. *"The demo is tomorrow at 3 PM,"* he says.

---

## Scene: Friday sprint review — the burndown chart was fiction

Friday 2 PM. The sprint review is in the large meeting room. Mirror Mike has printed the burndown chart. It is a beautiful straight line from 43 to 0, finishing on Wednesday afternoon.

> **Attempt** Fails
>
> | teamMember    | risk                   | approach        | result |
> |---------------|------------------------|-----------------|--------|
> | Blueprint Ben | Partial Implementation | Code Review     | FAILED |
> | Mirror Mike   | Partial Implementation | Sprint Feedback | FAILED |

Checklist Charlie presents the User Registration feature. He shows the endpoint. He demonstrates the happy path — email, password, 201 response.

Bugfinder Betty asks: *"Can you show me the GDPR audit log entry that was created?"*

Checklist Charlie opens the audit log table in the database viewer. It is empty. He opens the application log. There is a `log.info` entry with an email address. *"It logs here,"* he says. *"It logs the registration event."*

Bugfinder Betty: *"The GDPR requirement is for entries in the audit log table with a retention policy. That is what Ka-Ching Corp's legal team reviewed. That is what they will ask to see."*

Checklist Charlie: *"The ticket said all registration events must be logged. I logged them."*
Pinky Princess: *"In the audit log. The ticket says audit log."*
Checklist Charlie: *"It also says audit log in the application log."*

Blueprint Ben looks at the ticket. He looks at the pull request. The code is clean. One fifth of the requirements are implemented.

> **Risk** Risk is active
>
> | name          |
> |---------------|
> | Blame Culture |

Mirror Mike looks at the burndown chart he printed. He looks at the empty audit log table. He picks up his phone and cancels the 3 PM Ka-Ching Corp call.

> **Sprint** Close sprint
>
> | sprint    |
> |-----------|
> | Sprint 14 |

> **Sprint** Verified velocity is
>
> | sprint    | expected |
> |-----------|----------|
> | Sprint 14 | 0        |

> **Sprint** Sprint status is
>
> | sprint    | expected |
> |-----------|----------|
> | Sprint 14 | FAILED   |

Forty-three reported points. Zero verified points. Sprint: FAILED.

Ka-Ching Corp's go-live is postponed two weeks. Their project manager sends a polite but firm email. The word *"contractual"* appears twice.

---

## Moral of the Story

**Velocity measures tickets closed. It does not measure criteria delivered.**

Sprint 14 reported 43 points delivered. Sprint 14 delivered 0 verified points. The burndown chart was technically accurate — the tickets were marked done on Wednesday. The burndown chart was completely misleading — not one ticket was actually complete.

The GDPR audit log was not a nice-to-have. It was a contractual and legal obligation. It was hiding inside a "completed" ticket, invisible to everyone except the one person who read all five acceptance criteria — and she was on sick leave.

The gap between *"I implemented what I understood"* and *"the specification was fully met"* was not visible to anyone until it was too late. That gap is where TrueDocTales lives.

- ✗ 43 reported points → 0 verified → sprint FAILED
- ✗ A green ticket with one criterion is not done — it is a documented partial implementation
- ✗ The GDPR audit log was a legal requirement hiding in plain sight

*Sprint 15 begins. Checklist Charlie picks up his first ticket.*
*He reads the first criterion. It sounds clear.*
*He does not read the others.*
