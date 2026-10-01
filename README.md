# TestPoint Backend

REST API for **TestPoint**, a web-based online quiz and assessment system built for CSE352 (Advanced Java Lab), Southeast University.

Instructors keep a question bank, publish timed quizzes, and grade short answers. Students join a class, sit an assigned quiz, and receive objective scores as soon as they submit. An administrator manages accounts and can see platform totals.

This service is the Spring Boot API. The browser client lives in the separate `testpoint-frontend` project and talks to this API with JSON.

## Course and student information

|              |                                                        |
| ------------ | ------------------------------------------------------ |
| Course       | Advanced Java Lab (CSE352), Section 03                 |
| Department   | Computer Science and Engineering, Southeast University |
| Submitted to | Miftahul Sheikh, Lecturer, Department of CSE           |

| SL  | Name              | Student ID    |
| --- | ----------------- | ------------- |
| 1   | Emon Hossain      | 2023000010093 |
| 2   | Md Sajjad Hossain | 2024000010009 |

## What this API does

- Register and log in instructors and students. Passwords are stored with BCrypt. Sessions are stateless JWTs.
- Admin accounts are not created through public registration. A demo admin is seeded on an empty database.
- Classes with a join code, plus enrollment by student email.
- A reusable question bank: multiple choice, true/false, and short answer.
- Quizzes with a start and end window, duration, attempt limit, passing marks, and optional question shuffle.
- Assignment of a published quiz to a class or to selected students.
- Timed attempts. Each attempt stores a snapshot of the questions so later edits to the bank do not change a paper already in progress.
- Automatic grading of multiple-choice and true/false answers. Blank short answers score zero. Other short answers wait for the instructor.
- Optional AI features, used only when configured: draft questions from a lecture PDF, grade short answers, and write a study summary after an attempt is fully graded.
- Analytics for a quiz (attempt count, average percent, pass rate, question accuracy) and a history for the signed-in student.

## Technology

| Piece     | Choice                                                |
| --------- | ----------------------------------------------------- |
| Java      | 17                                                    |
| Framework | Spring Boot 3.5 (Web, Validation, Data JPA, Security) |
| Database  | PostgreSQL (H2 when tests run)                        |
| Auth      | JWT (jjwt), BCrypt                                    |
| PDF text  | Apache PDFBox                                         |
| Build     | Maven                                                 |

The API listens on port **8081**.

## Requirements

- JDK 17 or later
- Maven
- PostgreSQL, with a database the app can update

Default connection values:

| Setting  | Default                                      |
| -------- | -------------------------------------------- |
| URL      | `jdbc:postgresql://localhost:5432/testpoint` |
| Username | `testpoint`                                  |
| Password | `testpoint`                                  |

## Run

```bash
mvn spring-boot:run
```

On an empty database the app seeds a class, a published sample quiz, and these accounts (local demo only):

| Role       | Email                      | Password       |
| ---------- | -------------------------- | -------------- |
| Admin      | admin@testpoint.local      | Admin@123      |
| Instructor | instructor@testpoint.local | Instructor@123 |
| Student    | student1@testpoint.local   | Student@123    |
| Student    | student2@testpoint.local   | Student@123    |

## Configuration

Set these environment variables when the defaults are not what you want:

| Variable       | Purpose                                                 |
| -------------- | ------------------------------------------------------- |
| `DB_URL`       | JDBC URL                                                |
| `DB_USERNAME`  | Database user                                           |
| `DB_PASSWORD`  | Database password                                       |
| `JWT_SECRET`   | HMAC key, at least 32 bytes                             |
| `CORS_ORIGINS` | Allowed browser origin. Default `http://localhost:3000` |
| `AI_ENABLED`   | `true` to turn on AI features                           |
| `AI_API_KEY`   | Key for the chat API                                    |
| `AI_BASE_URL`  | Default `https://api.openai.com/v1`                     |
| `AI_MODEL`     | Default `gpt-4o-mini`                                   |

AI question generation, short-answer grading, and study summaries return a clear error until `AI_ENABLED` and `AI_API_KEY` are set. Manual grading still works.

## Tests

```bash
mvn test
```

`AuthRulesTest` checks registration, login, and role rejection. `AttemptEvaluationTest` checks objective scoring and pending short answers.

## Main API groups

| Area               | Base path              |
| ------------------ | ---------------------- |
| Auth               | `/api/auth`            |
| Admin              | `/api/admin`           |
| Classes            | `/api/classes`         |
| Question bank      | `/api/questions`       |
| Quizzes            | `/api/quizzes`         |
| Instructor summary | `/api/instructor`      |
| Student quizzes    | `/api/student/quizzes` |
| Attempts           | `/api/attempts`        |
| Grading            | `/api/grading`         |
| Analytics          | `/api/analytics`       |

Send `Authorization: Bearer <token>` on every route except register and login.
