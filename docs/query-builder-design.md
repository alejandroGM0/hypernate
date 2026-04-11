# Fluent Rich Query Builder — Design Document

## What this is

My solution for the coding challenge in [issue #86](https://github.com/LF-Decentralized-Trust-Mentorships/mentorship-program/issues/86). I designed and partially implemented a fluent rich query builder API that integrates into Hypernate's `Registry` to abstract CouchDB Mango selector queries.

## How it works

The entry point is `registry.richQuery(Asset.class)`, which returns a `RichQueryBuilder`. From there the user chains predicates and options using a fluent API:

```java
List<Asset> results = registry.richQuery(Asset.class)
    .where("color").is("blue")
    .and("size").greaterThan(10)
    .and("owner").in("Alice", "Bob")
    .sortBy("value", SortOrder.DESC)
    .limit(50)
    .execute();
```

Nothing touches the Fabric stub until `.execute()` is called — the builder just accumulates state.

## Classes

```
Registry
  └── richQuery(Class<T>) → RichQueryBuilder<T>

RichQueryBuilder<T>        — main builder, holds Selector, calls stub.getQueryResult()
FieldPredicate<T>          — returned by where()/and(), captures field name, resolves via is()/greaterThan()/etc.
Selector                   — package-private, builds the CouchDB JSON from accumulated conditions
SortOrder                  — enum (ASC, DESC)
QueryException             — extends DataAccessException
```

## Why I made these choices

**Two-object fluent chain (Builder + FieldPredicate):** Calling `.where("color")` returns a `FieldPredicate` that knows the field name. Then `.is("blue")` registers the condition and hands control back to the builder. This gives a natural `.where("x").is(y)` reading without needing method overloads.

**Selector is package-private:** It's an implementation detail — users only interact with `RichQueryBuilder` and `FieldPredicate`. Internally it uses a `LinkedHashMap` so field order is preserved (makes testing predictable), and `computeIfAbsent()` lets you stack multiple operators on the same field (e.g. `size > 10 AND size < 100`).

**$eq shorthand:** CouchDB allows `{"color": "blue"}` instead of `{"color": {"$eq": "blue"}}`. The Selector detects single `$eq` conditions and uses the short form.

**QueryException:** Extends the existing `DataAccessException` so query errors fit into Hypernate's exception hierarchy. Uses Lombok's `@StandardException`.

**Uncommitted writes:** Rich queries go straight to CouchDB and skip `WriteBackCachedStubMiddleware`'s cache. This is a Fabric thing (execute-order-validate), not a bug. I documented it in the Javadoc so users know about it.

## What I implemented

The challenge asked for "one specific function." I implemented `execute()` — it's the function that actually proves the whole design works end-to-end:

1. Builds the Mango JSON via `Selector.toJSON()`
2. Calls `stub.getQueryResult(json)`
3. Iterates and deserializes results with `JSON.deserialize()`
4. Returns a `List<T>`

I also implemented all 10 operators in `FieldPredicate` since they're one-liners that just register conditions — they're part of the design, not separate features.

## Tests

21 tests using JUnit 5 + Mockito + AssertJ:

- **16 selector tests** — every operator, multi-field queries, same-field merging, sort/limit/skip
- **2 validation tests** — bad limit and skip values
- **3 execution tests** — empty results, deserialization, lazy evaluation

```bash
./gradlew test --tests "hu.bme.mit.ftsrg.hypernate.registry.query.*"
```
