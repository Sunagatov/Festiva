# Festiva verification guide

Pick the smallest verification that gives confidence.

## Basic commands

### Run all tests
```bash
mvn test
```

### Run one test class
```bash
mvn -Dtest=ClassName test
```

### Run one test method
```bash
mvn -Dtest=ClassName#methodName test
```

## Local runtime

### Start Mongo only
```bash
docker compose up -d mongo
```

### Run the app from shell
```bash
set -a && source .env && set +a && mvn spring-boot:run
```

### Run the bot container profile
```bash
docker compose --profile bot up -d --build
```

## Regression checklist by change type

### Routing / command handling
- unknown text behavior
- command dispatch
- stateful message handling
- document handling if relevant

### Callback changes
- valid callback path
- invalid callback value
- parse failure path
- session-expired path

### Friend validation
- blank name rejected
- name too long rejected
- duplicate normalized name rejected
- invalid date rejected
- unknown-year birthday still accepted

### Reminder changes
- timezone handling
- notify hour handling
- lastNotifiedDate duplicate suppression
- leap day birthday behavior
- notifyEnabled false path

### ICS import changes
- wrong file type rejected
- oversized file rejected
- empty / invalid ICS rejected
- duplicate names filtered
- pending import confirm/cancel works
- AI failure falls back safely

### i18n changes
- EN path still works
- RU path still works
- placeholders render correctly
- HTML escaping remains correct

## What not to skip

Even for small fixes, do not skip tests when you touched:
- callback parsing
- date logic
- duplicate handling
- reminder behavior
- session clearing
