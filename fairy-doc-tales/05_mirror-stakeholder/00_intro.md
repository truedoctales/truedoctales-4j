# The Stakeholder Asks the Mirror

## Overview

This chapter tells the story of a **Stakeholder** who uses the project documentation as his mirror — and always asks it the same question: *"Mirror, mirror on the wall, who writes the best stories of all?"*

The answer, of course, is never the developer.

The story shows what happens when requirements are vague, assumptions are never validated, and blame replaces responsibility.

## The Problem

A feature is requested. A story is written — briefly, informally, with good intentions. The developer reads it and builds what they understand. The stakeholder reviews the result and immediately asks: *why did you not do what I said?*

Nobody checks the story. Nobody validates the expectation. The mirror only reflects blame.

## What Goes Wrong

- ✗ Requirements are written without concrete examples
- ✗ No one validates that the developer's understanding matches the intent
- ✗ The system is delivered — but not what was expected
- ✗ The stakeholder blames the developer
- ✗ The developer points to the story as proof they did their job
- ✗ Trust breaks down, and the next sprint begins in the same fog

## Story Structure

```mermaid
graph TD
    A[Stakeholder Requests Feature] --> B[Product Owner Writes Vague Story]
    B --> C[Developer Interprets Story]
    C --> D[Developer Implements Their Understanding]
    D --> E[Stakeholder Reviews Delivery]
    E --> F{Does it match expectation?}
    F -->|No| G[Blame the Developer]
    G --> H[Developer Points to Documentation]
    H --> I[Both Are Right. Both Are Wrong.]
    I --> J[Project Delayed. Trust Lost.]
    F -->|Yes| K[This Never Happens]
    style G fill:#f66
    style I fill:#f66
    style J fill:#f66
    style K fill:#aaa
```

*"Mirror, mirror, who wrote it wrong?"*
*"Everyone did — and no one knows."*
