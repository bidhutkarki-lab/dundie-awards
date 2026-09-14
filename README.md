# Employee directory organization

This is an application for managing employees of a company. Employees belong to organizations within the company.

As recognition, employees can receive Dundie Awards.

* A `Dundie Award` is in reference to the TV show [The Office](https://en.wikipedia.org/wiki/The_Dundies) in which the main character hands out awards to his colleagues. For our purposes, it's a generic award.

## Running with Docker

```bash
docker compose up --build
```

This starts Postgres and the application. The app is available at <http://localhost:3000>.

Data is persisted in the `postgres-data` volume. To start from a clean database:

```bash
docker compose down -v
```

## Running locally without Docker

The app expects a Postgres instance. Start just the database with `docker compose up -d db`,
then run `./gradlew bootRun`. Connection settings can be overridden with the
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`
environment variables.

## Tests

```bash
./gradlew test
```

Tests spin up a throwaway Postgres via Testcontainers, so Docker must be running. No
separate database setup is needed — the container is created and torn down per run.

## API docs

With the app running, the OpenAPI schema is at <http://localhost:3000/openapi> and the
Swagger UI at <http://localhost:3000/swagger-ui.html>.

## Instructions

In preparation for the upcoming call with NinjaOne, `clone` this repo and run it locally.

![success](success.png)

Become familiar with the application and it's characteristics. Use your favorite HTTP Client (like [Postman](https://www.postman.com/)) to exercise the endpoints and step through the code to help you get to know the application.

In the call, we will introduce new code to the application, and you will comment on issues with the endpoint. Please be ready to share your screen in the call with us with the application ready to run.

**Bonus:** Spot any issues or potential improvements you notice in the application while you're familiarizing yourself and make note of them for our call. We would love to see your input in how to make this application better.
