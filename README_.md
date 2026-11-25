# 🏋️ Gym Membership System — Java OOP Assessment

## Overview

In this assessment you will design and implement a gym membership system in Java. The system manages two distinct branches of people **Members** and **Staff** through a shared class hierarchy rooted at `Person`.

Your task is to implement all classes described below, demonstrating the four core OOP principles:

| Principle | Where you'll apply it                                                                    |
|---|------------------------------------------------------------------------------------------|
| **Encapsulation** | Private fields, controlled getters/setters, immutable fields                             |
| **Inheritance** | Two-level hierarchy: `Person` → `Member`/`Staff` → concrete types                        |
| **Polymorphism** | `getRole()`, `getMonthlyFee()`, `getMonthlySalary()` behave differently per type         |
| **Abstraction** | `Person`, `Member`, and `Staff` are all abstract concrete types must fulfil the contract |

## Assessment Structure

This assessment has three components. You may complete them in any order.

| Component | Weight | Recommended Time |
|---|---|---|
| Implementation | **50%** | 2 hours |
| Comprehensive Long Question  | **50%** | 1 hour |

### Scoring

```
Coding Score  = (tests passed / total tests) × 50%
Long Q Score= (marks earned / total UML marks) × 50%

Final Score   = Coding Score +  Long Q Score
```


---

## Class Hierarchy

```
Person  (abstract)
├── Member  (abstract)
│   ├── BasicMember
│   ├── PremiumMember
│   └── StudentMember
└── Staff  (abstract)
    ├── Trainer
    └── Receptionist
```

---

## Project Structure

```
we-gym/
  pom.xml
  src/
    main/java/za/co/wethinkcode/
      Main.java                            
      model/
        Person.java                       
        Member.java                   
        BasicMember.java                  
        PremiumMember.java                  
        StudentMember.java                  
        Staff.java                          
        Trainer.java                        
        Receptionist.java                   
      service/
        GymManager.java                     
    test/java/za/co/wethinkcode/
      PersonTest.java
      MemberTest.java
      StaffTest.java
      GymManagerTest.java
```

> **Do NOT modify any test files or `Main.java`.** All your work goes in `model` and `service`.

---

## Getting Started

```bash
mvn compile
```

5. Run the test suite at any point to check your progress:

```bash
mvn test
```

---

## Implementation Steps

Work through the steps **in order**, each class builds on the ones before it.

---

### Step 1 — Implement `Person` (Abstract)

**File:** `src/main/java/za/co/wethinkcode/model/Person.java`

`Person` is the root of the entire hierarchy. Every person in the system — whether a member or a staff member — is a `Person`.

#### Fields

| Field         | Type     | Details                   |
|---------------|----------|---------------------------|
| `firstName`   | `String` | Private and **immutable** |
| `lastName`    | `String` | Private and **immutable** |
| `email`       | `String` | Private                   |
| `phoneNumber` | `String` | Private                   |

#### Constructor

Accepts `firstName`, `lastName`, `email`, `phoneNumber`.

#### Methods

| Method                                         | Details                                                             |
|------------------------------------------------|---------------------------------------------------------------------|
| `firstName()`                                  | Returns first name.                                                 |
| `lastName()`                                   | Returns last name.                                                  |
| `email()` <br/> `addEmail(String)`             | Getter and setter.                                                  |
| `phoneNumber()` <br/> `addPhoneNumber(String)` | Getter and setter.                                                  |
| `fullName()`                                   | Returns `firstName + " " + lastName`.                               |
| `toString()`                                   | Returns a summary including full name and email.                    |
| `role()`                                       | **Abstract.** Every subclass must provide its own role description. |

---

### Step 2 — Implement `Member` (Abstract)

**File:** `src/main/java/za/co/wethinkcode/model/Member.java`

`Member` extends `Person` and adds membership-specific state. It stays abstract because the fee and access level vary by membership type.

#### Additional Fields

| Field      | Type      | Details                      |
|------------|-----------|------------------------------|
| `memberId` | `String`  | Private and **immutable**.   |
| `active`   | `boolean` | Private; defaults to `true`. |

#### Constructor

Accepts `firstName`, `lastName`, `email`, `phoneNumber`, `memberId`. Calls `super()`. Sets `active` to `true`.

#### Methods

| Method                                   | Details                                                                     |
|------------------------------------------|-----------------------------------------------------------------------------|
| `memberId()`                             | Returns member ID. No setter.                                               |
| `isActive()` <br/> `makeActive(boolean)` | Getter and setter.                                                          |
| `toString()`                             | Extends parent summary with `memberId` and active status.                   |
| `monthlyFee()`                           | **Abstract.** Returns the monthly fee in Rands.                             |
| `accessLevel()`                          | **Abstract.** Returns a description of gym areas accessible to this member. |

---

### Step 3 — Implement `BasicMember`, `PremiumMember`, `StudentMember`

All three extend `Member` and implement every abstract method.

> In all three: use the `@Override` annotation on every overridden method.

---

#### `BasicMember`

|                 | Details                                           |
|-----------------|---------------------------------------------------|
| Extends         | `Member`                                          |
| Extra fields    | None                                              |
| `MONTHLY_FEE`   | `public static final double MONTHLY_FEE = 299.00` |
| `monthlyFee()`  | Returns `MONTHLY_FEE`                             |
| `accessLevel()` | Returns `"Gym floor and cardio area only"`        |
| `role()`        | Returns `"Basic Member"`                          |
| `toString()`    | Includes `"Basic Member"` and parent summary      |

---

#### `PremiumMember`

|                 | Details                                                                                |
|-----------------|----------------------------------------------------------------------------------------|
| Extends         | `Member`                                                                               |
| Extra field     | `personalTrainerAssigned (boolean)` — private, defaults to `false`. Getter and setter. |
| `MONTHLY_FEE`   | `public static final double MONTHLY_FEE = 699.00`                                      |
| `monthlyFee()`  | Returns `MONTHLY_FEE`                                                                  |
| `accessLevel()` | Returns `"Full access: gym floor, cardio, classes, and pool"`                          |
| `role()`        | Returns `"Premium Member"`                                                             |
| `toString()`    | Includes `"Premium Member"` and whether a trainer is assigned                          |

---

#### `StudentMember`

|                 | Details                                                                       |
|-----------------|-------------------------------------------------------------------------------|
| Extends         | `Member`                                                                      |
| Extra field     | `studentNumber (String)` — private and **immutable**. Getter only, no setter. |
| Constructor     | Accepts all `Member` params plus `studentNumber`                              |
| `MONTHLY_FEE`   | `public static final double MONTHLY_FEE = 149.00`                             |
| `monthlyFee()`  | Returns `MONTHLY_FEE`                                                         |
| `accessLevel()` | Returns `"Gym floor only"`                                                    |
| `role()`        | Returns `"Student Member"`                                                    |
| `toString()`    | Includes `"Student Member"` and the student number                            |

> **Design Hint - Polymorphism:** Because `BasicMember`, `PremiumMember`, and `StudentMember` all extend `Member`, you can store any of them in a `Member` variable and call `getMonthlyFee()` or `getAccessLevel()`. Java will automatically call the right version at runtime. This is polymorphism in action.

---

### Step 4 — Implement `Staff` (Abstract)

**File:** `src/main/java/za/co/wethinkcode/model/Staff.java`

`Staff` extends `Person` and adds employment-specific state. Like `Member`, it stays abstract because salary and duties differ per role.

#### Additional Fields

| Field            | Type     | Details                    |
|------------------|----------|----------------------------|
| `employeeId`     | `String` | Private and **immutable**. |
| `yearsOfService` | `int`    | Private                    |

#### Constructor

Accepts `firstName`, `lastName`, `email`, `phoneNumber`, `employeeId`, `yearsOfService`. Calls `super()`.

#### Methods

| Method                                              | Details                                                                           |
|-----------------------------------------------------|-----------------------------------------------------------------------------------|
| `employeeId()`                                      | Returns employee ID. No setter.                                                   |
| `yearsOfSevice()` <br/> `updateYearsOfService(int)` | Getter and Setter. Setter throws `IllegalArgumentException` if value is negative. |
| `toString()`                                        | Extends parent summary with `employeeId` and `yearsOfService`.                    |
| `monthlySalary()`                                   | **Abstract.** Returns monthly gross salary.                                       |
| `duties()`                                          | **Abstract.** Returns a description of key responsibilities.                      |

---

### Step 5 — Implement `Trainer` and `Receptionist`

Both extend `Staff` and implement every abstract method.

---

#### `Trainer`

|                   | Details                                                                        |
|-------------------|--------------------------------------------------------------------------------|
| Extends           | `Staff`                                                                        |
| Extra fields      | `specialisation (String)` — mutable. `certification (String)` — **immutable**. |
| `BASE_SALARY`     | `public static final double BASE_SALARY = 18_000.00`                           |
| `SALARY_PER_YEAR` | `public static final double SALARY_PER_YEAR = 500.00`                          |
| `monthlySalary()` | Returns `BASE_SALARY + (yearsOfService × SALARY_PER_YEAR)`                     |
| `duties()`        | Returns `"Personal training sessions and fitness assessments"`                 |
| `role()`          | Returns `"Trainer (" + specialisation + ")"`                                   |
| `toString()`      | Includes specialisation and certification                                      |

---

#### `Receptionist`

|                   | Details                                                                 |
|-------------------|-------------------------------------------------------------------------|
| Extends           | `Staff`                                                                 |
| Extra field       | `shift (String)` - `"Morning"`, `"Afternoon"`, or `"Evening"`. Mutable. |
| `MONTHLY_SALARY`  | `public static final double MONTHLY_SALARY = 12_000.00`                 |
| `monthlySalary()` | Returns `MONTHLY_SALARY` - fixed, regardless of years of service        |
| `duties()`        | Returns `"Member check-in, phone enquiries, and administrative tasks"`  |
| `role()`          | Returns `"Receptionist (" + shift + " shift)"`                          |
| `toString()`      | Includes shift information                                              |

---

### Step 6 — Implement `GymManager`

**File:** `src/main/java/za/co/wethinkcode/service/GymManager.java`

`GymManager` ties the system together. The reporting methods here are where **polymorphism is most visible** — they work with `Member` and `Staff` references, calling overridden methods without knowing the concrete type.

#### Fields

| Field       | Type           | Details                          |
|-------------|----------------|----------------------------------|
| `gymName`   | `String`       | Private and immutable.           |
| `members`   | `List<Member>` | Private. All registered members. |
| `staffList` | `List<Staff>`  | Private. All employed staff.     |

#### Constructor

Accepts `gymName`. Initialises both lists as empty `ArrayList`s.

#### Methods

| Method                          | Details                                                                                |
|---------------------------------|----------------------------------------------------------------------------------------|
| `addMember(Member)`             | Adds a member to the list.                                                             |
| `removeMember(String memberId)` | Removes by ID. Throws `IllegalArgumentException` if not found.                         |
| `findMember(String memberId)`   | Returns the `Member` or `null` if not found.                                           |
| `members()`                     | Returns `Collections.unmodifiableList(members)`.                                       |
| `activeMembers()`               | Returns a **new** list of members where `isActive()` is `true`.                        |
| `addStaff(Staff)`               | Adds a staff member.                                                                   |
| `findStaff(String employeeId)`  | Returns the `Staff` or `null` if not found.                                            |
| `staffList()`                   | Returns `Collections.unmodifiableList(staffList)`.                                     |
| `totalMonthlyMemberRevenue()`   | Sums `getMonthlyFee()` for all **active** members. Returns `0.0` if none.              |
| `totalMonthlyStaffCost()`       | Sums `getMonthlySalary()` for all staff. Returns `0.0` if none.                        |
| `printAllRoles()`               | Calls `getRole()` on every `Person` (members + staff) and prints each to `System.out`. |
| `gymName()`                     | Returns the gym name.                                                                  |

---

## Comprehension Questions
 
Answer all five questions in the `answers.txt` file provided in your repository. Write in full sentences. Where questions ask for code examples, keep them short and focused.
 
---
 
### Question 1 — Object-Oriented Programming **(10 marks)**
 
Your team lead is reviewing your code and asks you to walk them through the four core OOP principles — Encapsulation, Abstraction, Inheritance, and Polymorphism. For each principle, explain what it is, how you would identify it when reading through a codebase, and what it is used for.
 
 
---
 
### Question 2 — Problem Statements **(10 marks)**
 
You have just joined a development team and been handed a vague brief for a new feature. A senior developer tells you to write a problem statement before writing a single line of code. Explain what a problem statement is, what it should contain, and why it is important to define it clearly before development begins.
 
 
---
 
### Question 3 — Test-Driven Development **(10 marks)**
 
A junior developer on your team has never heard of TDD and asks you to explain it. Describe the process and the cycle that governs it, what happens at each stage, why tests are written before the implementation, and what effect this has on the quality and design of the code produced.
 
 
---
 
### Question 4 — The Super Constructor **(10 marks)**
 
A classmate is getting a compilation error they cannot explain — their subclass constructor is failing and they do not understand why. Explain what the super constructor is, why it exists, and how it is used. Use a short code example to support your explanation.
 
 
---
 
 
### Question 5 — Robot Worlds: Technical Reflection **(10 marks)**
 
You worked on a Greenfield project called Robot Worlds during this programme. Describe the project from a technical perspective. Your answer should address the key concepts and decisions that governed the system; such as the architecture, communication between components, how OOP principles were applied, and how correctness was ensured through testing.
 
Do not describe minor robot behaviours. Focus on the technical foundations that held the system together.
 

*Keep the gains coming! 💪*