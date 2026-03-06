<div align="center">
  <br>
  <h1>🎂 Festiva</h1>
  <p><strong>A Telegram birthday reminder bot — never forget a friend's birthday again.</strong></p>
  <p>
    <a href="https://t.me/zufarexplained">💬 Community</a> ·
    <a href="https://github.com/Sunagatov/Festiva/issues?q=is%3Aopen+label%3A%22good+first+issue%22">🟢 Good First Issues</a> ·
    <a href="https://github.com/Sunagatov/Festiva/issues">🐛 Issues</a>
  </p>

  [![License: CC BY-NC 4.0](https://img.shields.io/badge/license-CC%20BY--NC%204.0-lightgrey.svg)](LICENSE)
  [![GitHub Stars](https://img.shields.io/github/stars/Sunagatov/Festiva)](https://github.com/Sunagatov/Festiva/stargazers)
  [![Contributors](https://img.shields.io/github/contributors/Sunagatov/Festiva)](https://github.com/Sunagatov/Festiva/graphs/contributors)
</div>

---

## 🚀 Quick Start

**📋 Prerequisites:** Java 25, Maven 3.9+, Docker Desktop, Telegram Bot Token (from [@BotFather](https://t.me/BotFather))

```bash
# 1. 📥 Clone
git clone https://github.com/Sunagatov/Festiva.git && cd Festiva

# 2. 🔧 Fill in your credentials
# edit TELEGRAM_BOT_TOKEN and TELEGRAM_BOT_USERNAME in .env
```

---

### Option A — IntelliJ + infra in Docker *(recommended for development)*

```bash
# Start only MongoDB
docker compose up -d mongo
```

Then run the app from IntelliJ using the **EnvFile plugin** pointed at `.env`, or from the terminal:

```bash
# Linux / macOS:
export $(cat .env | xargs) && mvn spring-boot:run
```

---

### Option B — Everything in Docker

```bash
# Start MongoDB + the bot together
docker compose up -d mongo
docker compose --profile bot up -d --build
```

**Production (MongoDB Atlas):**
```bash
# Fill in .env.prod, then:
docker compose -f docker-compose.prod.yml up -d --build
```

---

**🧪 Run the tests:**
```bash
mvn test
```
✅ Tests use Testcontainers — Docker must be running.

---

## 🤔 What is this?

Festiva is a Telegram bot that helps you manage and receive birthday reminders for your friends. Add friends with their birthdates, get notified on the day, the day before, and a week in advance. Supports Russian and English, milestone (jubilee) birthday highlights, and month-by-month birthday browsing.

**🔔 Automatic reminders** fire daily at 09:00 for birthdays today, tomorrow, and in 7 days.

---

## 🛠️ Tech Stack

| 📂 Category | 🔧 Technology |
|---|---|
| 💻 Language | Java 25 |
| 🏗️ Framework | Spring Boot 4.0, Spring Scheduling, Spring Actuator |
| 🗄️ Database | MongoDB Atlas, Spring Data MongoDB |
| 📨 Messaging | Apache Kafka (optional metrics) |
| 🤖 Telegram | telegrambots-springboot-longpolling-starter 9.0 |
| 🧪 Testing | JUnit 5, Mockito, Testcontainers, AssertJ |
| 🚢 Deployment | Docker |

---

## ✨ Features

- 🔔 **Automatic reminders** — daily at 09:00 for birthdays today, tomorrow, and in 7 days
- 🎉 **Milestone highlights** — jubilee birthdays (multiples of 5) are called out specially
- 📅 **Month-by-month browsing** — scroll through birthdays by month
- 🌍 **Bilingual** — full support for 🇬🇧 English and 🇷🇺 Russian
- 👥 **Friend management** — add and remove friends with their birthdates

---

## 🤖 Commands

| 🎯 Command | 📝 Description |
|---|---|
| `/start` | Welcome message and command overview |
| `/add` | Add a friend with their birthdate |
| `/remove` | Remove a friend |
| `/list` | List all friends sorted by birthday |
| `/birthdays` | Browse birthdays by month |
| `/upcomingbirthdays` | Birthdays in the next 30 days |
| `/jubilee` | Upcoming milestone birthdays (multiples of 5) |
| `/language` | Switch between 🇬🇧 English and 🇷🇺 Russian |
| `/cancel` | Cancel the current operation |

---

## 📁 Project Structure

```
src/main/java/com/festiva/
├── 🤖 bot/            # BirthdayBot, CallbackQueryHandler
├── 💬 command/        # CommandRouter, all command handlers
├── 👥 friend/         # Friend entity, FriendService, MongoDB repository
├── 🌐 i18n/           # Lang enum, Messages (EN + RU)
├── 📊 metrics/        # Kafka metrics sender
├── 🔔 notification/   # BirthdayReminder scheduler
└── 🗂️ state/          # UserStateService, BotState
```

---

## ⚙️ Environment Variables

| Variable | Required | Description |
|---|---|---|
| `MONGO_URI` | ✅ | MongoDB connection URI |
| `MONGO_DATABASE_NAME` | ❌ | Defaults to `FestivaDatabase` |
| `TELEGRAM_BOT_TOKEN` | ✅ | Token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | ✅ | Your bot's username |
| `APP_KAFKA_ENABLED` | ❌ | `true` to enable Kafka metrics |
| `KAFKA_BOOTSTRAP_SERVERS` | ❌ | Kafka server (metrics only) |
| `KAFKA_API_KEY` | ❌ | Kafka API key |
| `KAFKA_API_SECRET` | ❌ | Kafka API secret |

See `.env` for local defaults and `.env.prod` for the production template.

---

## 🤝 Contributing

🎉 Contributions are welcome.

| 🎯 Situation | 🚀 Action |
|---|---|
| 🐛 Found a bug | [Open an issue](https://github.com/Sunagatov/Festiva/issues/new) with the `bug` label |
| 💡 Want a feature | Start a [Discussion](https://github.com/Sunagatov/Festiva/discussions) first |
| 👨💻 Ready to code | Pick a [`good first issue`](https://github.com/Sunagatov/Festiva/issues?q=is%3Aopen+label%3A%22good+first+issue%22), comment "I'm on it" |
| 🔧 Big change | Comment on the issue before writing code — tickets may have hidden constraints |

---

## 📄 License

📜 [CC BY-NC 4.0](LICENSE) — free for educational and personal use with author attribution. Commercial use requires explicit written permission from the author ([zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)).

---

## 📞 Contact

- 💬 **Telegram community:** [Zufar Explained IT](https://t.me/zufarexplained)
- 👤 **Personal Telegram:** [@lucky_1uck](https://web.telegram.org/k/#@lucky_1uck)
- 📧 **Email:** [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 🐛 **Issues:** [GitHub Issues](https://github.com/Sunagatov/Festiva/issues)

❤️
