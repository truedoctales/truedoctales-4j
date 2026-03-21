# The Villains — They Were Never on the Risk Register

Nobody planned for them. Nobody named them in the kickoff. They do not appear in any status report — until it is too late.

They grow quietly, in the gap between what is written and what is built.

## Scene: The invisible threats take shape

They do not roar. They do not fight with fists. They simply expand, silently, until a sprint review or an audit or a customer call makes them impossible to ignore.

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

## Where each villain lives and how it grows

| Villain                  | Where It Hides                          | How It Grows                                                         |
|--------------------------|-----------------------------------------|----------------------------------------------------------------------|
| Documentation Drift      | Between the spec and the last commit    | Every change that updates the code but not the story adds to the gap |
| Missing Acceptance Test  | Inside every ticket marked "done"       | Features built without a verified example of correct behaviour       |
| Blame Culture            | In retrospectives without shared facts  | When there is no shared evidence, everyone defends their own version |
| Unimplemented Feature    | In the product catalogue                | Features described as live that were never assigned to development   |
| Audit Failure            | Waiting for the worst possible moment   | External verification reveals the gap that internal trust concealed  |
| Partial Implementation   | Inside a green ticket                   | One criterion done, four ignored, the ticket closed anyway           |

## What defeats them

Each villain has exactly one structural weakness — not a policy, not a process improvement, not a longer retrospective.

| Villain                  | Weakness                        | What That Looks Like in Practice                                   |
|--------------------------|---------------------------------|--------------------------------------------------------------------|
| Documentation Drift      | Executable Specifications       | The story runs as a test. If it fails, the drift is visible today. |
| Missing Acceptance Test  | Automated Verification          | Every criterion is a test. No green test, no done.                 |
| Blame Culture            | Shared Accountability           | The story is the shared truth. Everyone can read it and run it.    |
| Unimplemented Feature    | Living Documentation            | A feature not covered by a passing test cannot be listed as live.  |
| Audit Failure            | Verified Documentation          | The auditor runs the stories. Pass means proven. Fail means honest.|
| Partial Implementation   | Complete Specification Coverage | All criteria must be green before the ticket is closed.            |

*They are alive and well at FinTrack Solutions. For now.*
