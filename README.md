# Jev Comment Moderation

A reader leaves a comment on your blog. The app decides what to do with it: publish it, hold it for a person, or reject it.

It uses the Decision API that is new in Spring AI 2.1. You don't get a paragraph back that you have to parse. You ask a question, list the possible answers, and get a typed answer back. Then plain Java decides what happens. The model here is [TypeSafe Jev](https://docs.typesafe.ai/api).

## Tested with

Java 25, Spring Boot 4.2.0, Spring AI 2.1.0.

## What you need

- JDK 25 or later
- A TypeSafe API key

```bash
export TYPESAFE_API_KEY=your-key-here
```

The key goes into `spring.ai.systemone.api-key` in [application.yaml](src/main/resources/application.yaml). That is the only setup. The Spring AI starter creates the `DecisionModel` bean for you.

## Run it

```bash
./mvnw spring-boot:run
```

The app starts on port 8080.

## Try it

Open [requests.http](requests.http) in IntelliJ and run the requests, or use curl:

```bash
curl -X POST http://localhost:8080/comments \
  -H "Content-Type: application/json" \
  -d '{"text": "Great post!!! I made $5,000 last week with FREE crypto signals. DM me on Telegram to join."}'
```

You get back something like this:

```json
{"action":"REJECT","reason":"Spam","spamProbability":0.97,"topic":"Off topic","tone":"Neutral"}
```

The numbers change a little from run to run.

## The three kinds of questions

All three are asked in one call to Jev.

| Question | Type | What you get back |
| --- | --- | --- |
| Is this comment spam or self-promotion? | `YesNoQuestion` | The probability of yes |
| What kind of comment is this? | `ChoiceQuestion` | One of Question, Feedback or Off topic |
| How respectful is this comment? | `ScoreQuestion` | A level: Rude, Neutral or Kind |

## The rules are plain Java

The answers are typed, so the rules are ordinary `if` statements in `ModerationController`:

1. Jev refused to answer: **HOLD**
2. Spam probability of 0.9 or more: **REJECT**
3. Spam probability of 0.5 or more: **HOLD**
4. Tone is Rude: **HOLD**
5. Anything else: **PUBLISH**

You pick the lines, not the model. Try the last request in `requests.http`. A reader links to their own blog, and Jev gives it about 0.85. That is under the 0.9 line, so it is held for a person instead of rejected. Move the line and the behavior changes.

## Where to look

- [ModerationController.java](src/main/java/dev/danvega/moderation/ModerationController.java) holds the three questions, the rules and the `/comments` endpoint.
- [application.yaml](src/main/resources/application.yaml) holds the API key property.
- [ModerationControllerTest.java](src/test/java/dev/danvega/moderation/ModerationControllerTest.java) uses a fake Jev, so the tests run without an API key.

## Good to know

- Jev comes from the `spring-ai-starter-model-systemone` starter. With no model set, it uses `jev-latest`.
- Jev is not the only decision model in Spring AI 2.1. There are also OpenAI and Ollama ones. The controller only knows `DecisionModel`, so you can swap the starter without touching it.
- Decision models are machine learning models, not rule engines. A probability is not a guarantee. That is why unsure comments go to a person.
- If Jev refuses to answer a question, `getYesNo`, `getChoice` and `getScore` throw an exception. The app checks `response.isRefused(...)` first and holds the comment.
