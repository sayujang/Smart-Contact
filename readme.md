## 1️⃣ `@OneToMany(fetch = FetchType.LAZY)`

**Scenario:** A `User` has many `Contact`s.

```java
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Contact> contacts;
```

**Behavior:**

* The **entity (`User`) itself** is fully loaded immediately when you call `session.find(User.class, id)`.
* The **collection (`contacts`)** is lazy:

  * Hibernate does **not load contacts immediately**.
  * It creates a **proxy collection**.
  * SQL query for contacts is fired **only when you access `user.getContacts()`**.
* Example:

```java
User u = session.find(User.class, 101); // Query only for User
List<Contact> contacts = u.getContacts(); // Query fired now for contacts
```

* Important: If you never call `getContacts()`, **no SQL** for contacts is executed.

**Key point:** Lazy is **for a collection inside an entity**, not the entity itself.

---

## 2️⃣ `session.byId(Student.class).getReference(id)`

**Scenario:** You are loading a single entity lazily.

```java
Student s = session.byId(Student.class).getReference(101);
```

**Behavior:**

* The **entity itself** is lazy:

  * Hibernate returns a **proxy object**, not the fully initialized `Student`.
  * **No SQL is fired yet**.
* SQL query is fired **only when you access a property** like `s.getName()` or `s.getEmail()`.
* Example:

```java
Student s = session.byId(Student.class).getReference(101); // No query yet
System.out.println(s.getName()); // Query fired now for this Student
```

**Key point:** Lazy is **for the entity itself**, not a collection.

---

## ⚖️ Comparison Table

| Feature                    | `@OneToMany(fetch = LAZY)`               | `getReference()`                                   |
| -------------------------- | ---------------------------------------- | -------------------------------------------------- |
| What is lazy               | Collection inside entity                 | The entity itself                                  |
| Entity loaded immediately? | Yes (User)                               | No (Student proxy)                                 |
| SQL fired                  | Only when collection is accessed         | Only when entity property is accessed              |
| Proxy used?                | Proxy collection (`PersistentBag`, etc.) | Proxy entity (`Student` subclass)                  |
| Common use                 | Parent-child relationships               | Large entities, optional access, entity references |

---

### 🔑 Takeaway

* **Collection lazy (`OneToMany`)** → Entity is ready, collection not loaded yet.
* **Entity lazy (`getReference`)** → Entity itself is not loaded yet, only a proxy exists.

Think of it like this:

* `@OneToMany LAZY` → You have a **box**, the box is there but the **stuff inside the box** isn’t loaded yet.
* `getReference()` → You don’t even have the **box yet**, just a **placeholder**; the box appears only when you try to open it.


