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

JSP → Placed under src/main/webapp/WEB-INF/views/
Thymeleaf → Placed under src/main/resources/templates/

Advantages of @Builder

Readability – Each field is explicitly named.

Optional fields – You can skip fields you don’t want to set.

Immutability friendly – Often used with @Getter and final fields.

Avoids long constructors – No need to write multiple overloaded constructors.
🧭 How Spring Finds That File

Spring Boot (with Thymeleaf) uses a View Resolver behind the scenes — by default it’s configured like this:

spring.thymeleaf.prefix = classpath:/templates/
spring.thymeleaf.suffix = .html

Full Login Flow (Simplified)

User enters username + password in the login form.

Spring Security intercepts it.

It calls your AuthenticationProvider.

That provider calls SecurityCustomUserDetailService.loadUserByUsername().

That fetches user info from DB via UserRepo.

Password is compared using BCryptPasswordEncoder.

If valid → user is logged in and a session is created.
CSRF stands for Cross-Site Request Forgery.

It’s a type of web attack where a malicious website tricks a logged-in user into performing an unwanted action on another site — without their knowledge.

⚡ Example:

Suppose a user is logged in to your site smartcontact.com.

Now they visit a malicious site that secretly submits a form:

<form action="https://smartcontact.com/do-logout" method="post">
</form>
<script>document.forms[0].submit();</script>


If your app doesn’t have CSRF protection, that POST request could:

Delete their account,

Transfer money,

Or log them out —
all without the user realizing it.

How oauth2 works in flow

Client (your app) redirects user to Authorization Server (Google).

User (Resource Owner) logs in and grants permission.

Authorization Server issues a token to the Client.

Client uses token to access Resource Server (user’s data).

Client can now authenticate the user in your app without knowing their password.

clientId: 1085426913365-mc8f0dmp90dos9fc6cvot1pebhgt3jvo.apps.googleusercontent.com
client secret: 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

"sub" is per user per provider

Different users → Different sub values within the same provider.

Different providers → Even the same person will have different sub values for each provider.
sub identifies the end user not oauth2 client

spring.security.oauth2.client.registration.google.scope=profile,email  tells google which information our apps want to access from the user

