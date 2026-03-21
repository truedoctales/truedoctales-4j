# The Villains — They Were Never on the Risk Register

Nobody planned for them. Nobody named them in the kickoff. They were never assigned an owner, never tracked in Jira, never escalated to management.

They grew quietly — in the space between what was written and what was built.

## Scene: The invisible threats take shape

They are not dramatic. They do not announce themselves. By the time anyone notices them, they have already done the damage.

> **Monster** Create monster
>
> | id | name                    | threat  | weakness                        |
> |----|-------------------------|---------|---------------------------------|
> | 10 | Documentation Drift     | HIGH    | Executable Specifications       |
> | 11 | Missing Acceptance Test | HIGH    | Automated Verification          |
> | 12 | Blame Culture           | EXTREME | Shared Accountability           |
> | 13 | Unimplemented Feature   | EXTREME | Living Documentation            |
> | 14 | Audit Failure           | EXTREME | Verified Documentation          |
> | 15 | Partial Implementation  | HIGH    | Complete Specification Coverage |

> **Monster** Monster exists
>
> | name                    |
> |-------------------------|
> | Documentation Drift     |
> | Missing Acceptance Test |
> | Blame Culture           |
> | Unimplemented Feature   |
> | Audit Failure           |
> | Partial Implementation  |

> **Monster** Monster is alive
>
> | name                    |
> |-------------------------|
> | Documentation Drift     |
> | Missing Acceptance Test |
> | Blame Culture           |
> | Unimplemented Feature   |
> | Audit Failure           |
> | Partial Implementation  |

## What each villain actually does

They each attack a different part of the delivery chain. Together, they are devastating.

| Villain                  | Where It Strikes                             | How It Grows                                                        |
|--------------------------|----------------------------------------------|---------------------------------------------------------------------|
| Documentation Drift      | Between releases                             | Each change that updates the code but not the spec adds to the gap  |
| Missing Acceptance Test  | During development                           | Features built without verified examples of correct behaviour       |
| Blame Culture            | After a failure is discovered                | When there is no shared evidence, every person defends their version of the truth |
| Unimplemented Feature    | In the product catalogue                     | Features described, approved, never built — and never marked as missing |
| Audit Failure            | At the worst possible moment                 | External verification reveals the gap that internal trust concealed |
| Partial Implementation   | Inside a "completed" ticket                  | One criterion done, four ignored, the ticket marked green           |

## What defeats them

Each villain has one weakness — not a workaround, not a policy, not a longer retrospective. A structural solution.

| Villain                  | Weakness                        | What That Means in Practice                              |
|--------------------------|---------------------------------|----------------------------------------------------------|
| Documentation Drift      | Executable Specifications       | The story runs. If it fails, the drift is immediately visible. |
| Missing Acceptance Test  | Automated Verification          | Every acceptance criterion is a test. No test, no done.  |
| Blame Culture            | Shared Accountability           | The story is the shared truth. Everyone signed off on it. |
| Unimplemented Feature    | Living Documentation            | A feature that is not tested cannot be documented as available. |
| Audit Failure            | Verified Documentation          | The auditor runs the stories. Pass means proven. Fail means honest. |
| Partial Implementation   | Complete Specification Coverage | All criteria must be green before the ticket is closed.  |

They are alive and well at FinTrack Solutions. For now.
