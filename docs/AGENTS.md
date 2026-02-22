# Design Document Rules

## 1. Requirements / Ubiquitous Language

Applies to: `*requirements*.md`, `*ubiquitous-language*.md`

Hard constraints:

- DO NOT mention API, endpoints, HTTP, controllers, services.
- DO NOT mention DB, tables, schema, ERD, class names.
- Focus only on user scenarios and business rules.
- No implementation details.
- Requirements documents MUST include explicit scope-freeze statements.
- Output language: Korean.

## 2. Design Artifacts (Sequence / Class / ERD)

Applies to: design diagram documents under `docs/design/`

### 2.1 Common Rules

- All terminology MUST align with `00-ubiquitous-language.md`.
- All scenarios and rules MUST trace back to `01-requirements.md`.
- Use domain-level technical terms (class names, table names, relationships, attributes).
- DO NOT include framework implementation details (Spring annotations, JPA mappings, SQL queries).
- All diagrams MUST be written in Mermaid syntax embedded within `.md` documents.
- Use the `docs-with-mermaid` skill when creating diagram documents.
- Output language: Korean. Identifiers in diagrams may use English.

### 2.2 Sequence Diagram

Purpose: Visualize time-ordered message exchange between participants for a specific use case scenario.

Rules:

- One diagram = one scenario. Draw the main flow first, then separate diagrams for alternative/exception flows.
- Limit participants (lifelines) to 5–7. Split the diagram if exceeding 9.
- Order participants left-to-right by call sequence: Actor → Application Service → Domain → Repository.
- Use Aggregate Roots as participants, not internal entities. Hide intra-aggregate interactions.
- Fragment nesting (`alt`/`opt`/`loop`) MUST NOT exceed 2 levels. Use `ref` to extract deeper sub-scenarios.
- Show only business-meaningful return messages. Omit trivial returns to reduce clutter.
- Use `autonumber` for message numbering, `participant A as Alias` for long names, and `rect` for bounded context grouping.

UML 2.5.1 Notation (Chapter 17: Interactions):

- Lifeline identification: `name : Type` format (e.g., `customer : Customer`). Use `self` to represent the enclosing classifier.
- Message arrows:

  | MessageSort | Line | Arrowhead | Usage |
  |-------------|------|-----------|-------|
  | synchCall | solid | filled (solid) | Synchronous operation call |
  | asynchCall | solid | open | Asynchronous operation call |
  | reply | dashed | open | Return from synchronous call |
  | createMessage | dashed | open | Object creation |

- Ordering: A message MUST be sent before it is received. Events on the same Lifeline are totally ordered top-to-bottom.
- Messages MUST NOT cross CombinedFragment boundaries — both ends must be within the same fragment.
- Fragment operators:

  | Operator | Semantics |
  |----------|-----------|
  | `alt` | Choice — at most one operand executes. `[else]` = negation of all other guards. |
  | `opt` | Optional — equivalent to `alt` with one operand + empty. |
  | `loop` | `loop(min, max)` — guard checked after min iterations. Bare `loop` = 0..∞. |
  | `break` | Executes instead of the remainder of the enclosing fragment. |
  | `par` | Parallel — operands interleave freely, but ordering within each is preserved. |
  | `critical` | Atomic region — no interleaving on covered Lifelines. |
  | `ref` | Reference to another Interaction (InteractionUse). |

- Guards: `[boolean-expression]` or `[else]`, placed above the first event on the guarded Lifeline.
- Destruction: `X` at the bottom of a Lifeline. No further events may appear below it.

### 2.3 Class Diagram

Purpose: Visualize the domain model — aggregates, entities, value objects, and their relationships — at a business-concept level, NOT implementation level.

Rules:

- Domain model level only. Exclude framework annotations (`@Entity`), technical fields (`id`, `createdAt`, `updatedAt`, `deletedAt`), infrastructure classes (`JpaRepository`, `RepositoryImpl`).
- Mark DDD building blocks with stereotypes: `<<AggregateRoot>>`, `<<Entity>>`, `<<ValueObject>>`, `<<DomainService>>`, `<<Enumeration>>`.
- Use Mermaid `namespace` to express Aggregate boundaries.
- Relationships: Composition (`*--`) within an Aggregate, Association (`-->`) + ID type between Aggregates. Always annotate multiplicity.
- Include only business-meaningful attributes and domain behaviors (factory methods, state transitions, validations). Omit getters/setters, `equals()`/`hashCode()`, access modifiers.
- Limit to 15 classes per diagram. Split into overview (Aggregate-level) and detail (intra-Aggregate) diagrams.
- Mermaid caveat: Place `note` outside `namespace` blocks. Empty namespaces cause parse errors.

UML 2.5.1 Notation (Chapters 9, 11: Classification, Structured Classifiers):

- Class name: centered, boldface, uppercase first character. Abstract classes use italics or `{abstract}`.
- Compartments (top to bottom): name → attributes → operations. Suppressed compartments draw no separator line.
- Visibility markers: `+` public, `-` private, `#` protected, `~` package.
- Attribute format: `[visibility] [/] name : type [multiplicity] [= default] [{modifiers}]`. `/` = derived. Static = underlined.
- Operation format: `[visibility] name(params) : returnType`. Parameter direction: `in` (default) | `out` | `inout`.
- Relationships:

  | Relationship | Line | Arrowhead | Meaning |
  |-------------|------|-----------|---------|
  | Association | solid | none (or open for navigability) | Structural link between classifiers |
  | Composition | solid | filled diamond on whole side | Part lifecycle depends on whole; cascade delete |
  | Aggregation | solid | hollow diamond on whole side | Whole-part (weak); no lifecycle dependency |
  | Generalization | solid | hollow triangle → parent | "is-a" inheritance |
  | Realization | dashed | hollow triangle → interface | Interface implementation |
  | Dependency | dashed | open arrow → supplier | "uses" relationship |


- Multiplicity: placed near association ends without brackets. Common: `1`, `0..1`, `*` (= `0..*`), `1..*`.
- Stereotype: `<<keyword>>` centered above the class name (e.g., `<<interface>>`, `<<enumeration>>`).
- Navigability: open arrowhead = navigable end; small `x` = non-navigable end.

### 2.4 ERD

Purpose: Visualize data structure — entities, attributes, and relationships — at a logical design level to serve as a database blueprint.

Rules:

- Use Logical ERD level: show entities, attributes, PK/FK, and relationships. Use logical data types (`string`, `bigint`, `datetime`), not DBMS-specific types (`VARCHAR(255)`). Omit indexes.
- Default to 3NF. Intentional denormalization (e.g., order snapshot preserving price/product name at order time) is allowed only with a comment stating the business rationale.
- Entity names: singular English nouns (`PRODUCT`, not `PRODUCTS`). Attribute names: `snake_case`. FK format: `{referenced_entity}_id`.
- Resolve M:N relationships with a junction table. Promote to an independent entity if additional attributes exist (e.g., `ORDER_ITEM` with `quantity`, `unit_price`).
- Design with DDD Aggregate boundaries in mind: only Aggregate Roots are referenced externally via FK. Internal entities have lifecycle dependency (CASCADE) on the Root.
- Include soft-delete field (`deleted_at`) and audit fields (`created_at`, `updated_at`) following the BaseEntity pattern.
- Mermaid erDiagram caveats: NOT NULL, ENUM, INDEX, and composite PK are not directly supported — use comments (`"NN"`, `"ENUM: CREATED|COMPLETED"`) as supplements. Parentheses in data types are not
  allowed.
