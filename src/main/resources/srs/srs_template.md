# PROJECT NAME
## Software Requirement Specification

- Ho Chi Minh City, April 2026

## Table of Contents

1.  [Overview](#i-overview)
    1.  [Introduction](#1-introduction)
    2.  [Business Main Flows](#2-business-main-flows)
    3.  [Business Rules](#3-business-rules)
    4.  [System Functions](#4-system-functions)
2.  [System High Level Design](#ii-system-high-level-design)
    1.  [Logical Entity Relationship Diagram](#1-logical-entity-relationship-diagram)
    2.  [Database Design](#2-database-design)
3.  [Functional Requirements](#iii-functional-requirements)
    1.  [<<Feature Name 1>>](#1-feature-name-1)
        1.  [<<Function Name 1>>](#a-function-name-1)
        2.  [<<Function Name 2>>](#b-function-name-2)
    2.  [<<Feature Name 2>>](#2-feature-name-2)

## I. Overview

### 1. Introduction

[Content part 1: presents a high-level overview of the product and the environment in which it will be used, the users, and known constraints, assumptions, and dependencies]

[Content part 2: describes the product's context in the form of a context diagram in which you present the boundary and connections between the system you're developing and everything else in the universe. This identifies external entities (or terminators – software, hardware, human components, and other systems) outside the system that interface to it in some way, as well as data, control, and material flows between the terminators and the system]

<<Sample: The Cafeteria Ordering System is a new software system that replaces the current manual and telephone processes for ordering and picking up meals in the Process Impact cafeteria. The system is expected to evolve over several releases, ultimately connecting to the Internet ordering services for several local restaurants and to credit and debit card authorization services.>>

![Context Diagram](path/to/context_diagram.png)

### 2. Business Main Flows

[This part shows all the business main-flows have to be implemented to get the Goal of your Project. You can draw the Swimlane diagram for the business main-flows]

#### 2.1. Main-flow 01

[Swimlane diagram for main-flow 01 here]

![Swimlane Diagram 1](path/to/swimlane_diagram_1.png)

#### 2.2. Main-flow 02

[Swimlane diagram for main-flow 02 here]

![Swimlane Diagram 2](path/to/swimlane_diagram_2.png)

#### 2.3. Main-flow 03

[Swimlane diagram for main-flow 03 here]

![Swimlane Diagram 3](path/to/swimlane_diagram_3.png)

### 3. Business Rules

<<fill here with all the table of Business Rules...>>

| Business Rule ID | Business Rule Description                       |
| :----------------- | :--------------------------------------------- |
| BR-01              | Describe the business rule 01 content here   |
| BR-02              | Describe the business rule 02 content here   |
| BR-03              | Describe the business rule 03 content here   |

### 4. Use cases

[A use case (UC) describes a sequence of interactions between a system and an external actor that results in the actor being able to achieve some outcome of value. The names of use cases are always written in the form of a verb followed by an object. Select strong, descriptive names to make it evident from the name that the use case will deliver something valuable for some user]

#### 4.1. Use case Diagram(s)

[Provide the UC diagram(s) to show the actor-UCs and UC-UC relationships like the sample below. You can have multiple UC diagrams for the system]

![Use Case Diagram](path/to/use_case_diagram.png)

#### 4.2. Descriptions

[This part describes the use cases, you can follow the table form as below]

| ID   | Feature          | Use Case   | Use Case Description     |
| :--- | :--------------- | :--------- | :----------------------- |
| 01   | Menu Operations  | View Menu  | ...                      |
| 02   | Order Meals      | Order a Meal | ...                      |
| 03   | ...              | ...        | ...                      |

### 5. System Functions

#### 5.1. Screen Flow

[This part shows the system screens and the relationship among screens. You can draw the Screens Flow for the system in the form of diagram as below]

![Screen Flow Diagram](path/to/screen_flow_diagram.png)

#### 5.2. Screen Details

[Provide the descriptions for the screens in the Screens Flow above]

| #    | Feature     | Screen        | Description                 |
| :--- | :---------- | :------------ | :-------------------------- |
| 1    | Order Meals | Create Order  | <<Screen Brief description>> |
| 2    | Order Meals | Change Order  | ...                         |
| 3    | ...         | ...           | ...                         |

#### 5.3. User Authorization

[Provide the system roles authorization to the system features (down to screens, and event to the screen activities if applicable) in the table form as below – replace Role1, Role2, ... with the specific system user role names]

| Screen             | Role1 | Role2 | Role3 | Role4 | RoleX |
| :----------------- | :---- | :---- | :---- | :---- | :---- |
| <<Screen Name1>>   | X     |       |       | X     | X     |
| <<Screen Activity>> |       |       |       | X     | X     |
| <<Screen Name2>>   | X     | X     |       |       |       |
| Query All Data     | X     | X     |       |       |       |
| Query Own Data     |       |       |       |       | X     |
| Query Managed Data |       |       |       | X     |       |
| Add New Data       |       |       |       |       | X     |
| Update All Data    |       |       |       | X     |       |
| Update Own Data    |       |       |       |       | X     |
| Update Managed Data|       |       |       |       | X     |
| Delete Data        |       |       |       |       | X     |
| ...                |       |       |       |       |       |

In which:

*   Role1: <<role1 description>>
*   Role2: <<role2 description>>
*   ...

#### 5.4. Non-Screen Functions

[Provide the descriptions for the non-screen system functions, i.e batch/cron job, service, API, etc.]

| #  | Feature      | System Function | Description                    |
| :- | :----------- | :-------------- | :----------------------------- |
| 1  | <<Feature Name>> | <<Function Name1>>   | <<Function Name1 Description>> |
| 2  | ...          | ...             | ...                            |

## II. System High Level Design

### 1. Conceptual Entity Relationship Diagram

<<Draw the Conceptual Entity Relationship Diagram here showing all entities and relationship here...>>

![Conceptual ERD](path/to/conceptual_erd.png)

### 2. Logical Entity Relationship Diagram

<<Draw the Logical Entity Relationship Diagram here showing all entities, relationship of all entities, attributes, primary key, foreign key of each entity here...>>

![Logical ERD](path/to/logical_erd.png)

### 3. Database Design

[Provide the tables relationship like example below]

#### a. Database Schema

![Database Schema](path/to/database_schema.png)

#### b. Table Descriptions

| No   | Table         | Description                                                                                                                                                                                                         |
| :--- | :------------ |:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 01   | <Table name>  | <Description of the table> - Primary keys: <<list of primary key fields>> - Foreign keys: <<list of foreign key fields>>                                                                                            |
| 02   | <Table name2> | ...                                                                                                                                                                                                                 |


## III. Functional Requirements

### 1. <<Feature Name 1>>

#### a. <<Function Name 1>>

[A function can be a screen or a non-screen function (listed in the part 5.1 above). In this part, you need to provide the details on the related function, focus on mentioning below information

*   Function trigger: how this function is triggered (navigation path, a timing frequency, etc.
*   Function description: actors/roles, purpose, interface, data processing, etc.
*   Screen layout: mockup prototype of the screen, sample below is for Manage Products screen]

![Mockup Screen](path/to/mockup_screen.png)

*   Function Details: provide explanation for the data, validation, business logics, functionalities (for both normal cases and abnormal cases), etc. of the function so that the reader can image how it work.

#### b. <<Function Name 2>>

### 2. <<Feature Name 2>>