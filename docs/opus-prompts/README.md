# Opus Prompts for Complex Tasks

These prompts are designed to be fed to Claude Opus for complex architectural and algorithmic work that benefits from deeper reasoning.

## Prompts

| # | File | Task | Complexity | Priority |
|---|------|------|------------|----------|
| 1 | [01-recommendation-engine.md](./01-recommendation-engine.md) | Design personalized activity recommendations | Very High | P0 |
| 2 | [02-trust-score-algorithm.md](./02-trust-score-algorithm.md) | Design manipulation-resistant trust scoring | High | P1 |
| 3 | [03-social-graph-analysis.md](./03-social-graph-analysis.md) | Friend-of-friend discovery, community detection | Very High | P1 |
| 4 | [04-realtime-feed-ranking.md](./04-realtime-feed-ranking.md) | Real-time activity scoring and WebSocket updates | High | P2 |
| 5 | [05-fraud-abuse-detection.md](./05-fraud-abuse-detection.md) | Rule-based fraud/spam/harassment detection | High | P1 |

## How to Use

1. Copy the entire prompt file content
2. Paste into Claude Opus (or Claude with extended thinking)
3. Let it reason through the problem
4. Review output for:
   - SQL schema changes
   - Algorithm pseudocode
   - API contracts
   - Edge cases

## Context Shared Across Prompts

All prompts assume:
- **Stack:** Spring Boot 3 (Java 17) + PostgreSQL + React Native
- **Scale:** 100K users, Delhi NCR initially
- **Constraint:** No external ML services
- **Existing Schema:** Users, Activities, Participations, Contacts, Hubs

## Expected Outputs

Each prompt asks for:
1. High-level approach with trade-offs
2. Database schema changes (SQL)
3. Algorithm pseudocode
4. API contracts
5. Edge cases and failure modes
6. Performance considerations
7. Future improvements

## Order of Implementation

Recommended order based on dependencies and value:

```
1. Recommendation Engine (01)
   └── Enables "For You" feed

2. Trust Score Algorithm (02)
   └── Feeds into recommendations and access control

3. Social Graph Analysis (03)
   └── Enhances recommendations with mutual connections

4. Fraud Detection (05)
   └── Safety layer, can run in parallel

5. Real-Time Ranking (04)
   └── Polish layer, builds on top of recommendations
```

## After Opus Returns

1. Review the design document
2. Create implementation tickets
3. Start with database migrations
4. Implement service layer
5. Add API endpoints
6. Write tests
7. Deploy incrementally

---

*Created: November 24, 2024*
