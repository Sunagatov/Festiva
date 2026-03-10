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

> ⚠️ **Never commit `.env` with real credentials.** It is listed in `.gitignore` — keep it that way.

---

### Option A — IntelliJ + infra in Docker *(recommended for development)*

```bash
# Start only MongoDB
docker compose up -d mongo
```

Then run the app from IntelliJ using the **EnvFile plugin** pointed at `.env`, or from the terminal:

```bash
# Linux / macOS:
set -a && source .env && set +a && mvn spring-boot:run
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
| 🤖 Telegram | telegrambots-longpolling + telegrambots-client 9.0 |
| 🧪 Testing | JUnit 5, Mockito, Testcontainers, AssertJ |
| 🚢 Deployment | Docker |

---

## ✨ Features

- 🔔 **Automatic reminders** — daily at 09:00 for birthdays today, tomorrow, and in 7 days
- 🎉 **Milestone highlights** — jubilee birthdays (multiples of 5) are called out specially
- 📅 **Month-by-month browsing** — scroll through birthdays by month
- 🌍 **Bilingual** — full support for 🇬🇧 English and 🇷🇺 Russian
- 👥 **Friend management** — add, edit, remove friends with birthdates and relationship labels
- 📦 **Bulk import** — add many friends at once via CSV file or paste
- 📤 **Export** — download your friends list as a CSV file
- 🔍 **Search** — find friends by name
- 📊 **Stats** — see your birthday statistics
- ⚙️ **Settings** — configure notification hour and timezone
- 🗑️ **Account deletion** — remove all your data

---

## 🤖 Commands

| 🎯 Command | 📝 Description |
|---|---|
| `/start` | Welcome message and command overview |
| `/menu` | Show the command menu |
| `/help` | Alias for `/menu` |
| `/about` | About Festiva |
| `/add` | Add a friend with their birthdate |
| `/addmany` | Bulk-add friends via CSV file or paste |
| `/edit` | Edit a friend's name, date, or relationship |
| `/remove` | Remove a friend |
| `/list` | List all friends sorted by birthday |
| `/birthdays` | Browse birthdays by month |
| `/today` | See today's birthdays |
| `/upcomingbirthdays` | Birthdays in the next 30 days |
| `/jubilee` | Upcoming milestone birthdays (multiples of 5) |
| `/search` | Search friends by name |
| `/stats` | Birthday statistics |
| `/export` | Download friends list as CSV |
| `/settings` | Configure notification hour and timezone |
| `/language` | Switch between 🇬🇧 English and 🇷🇺 Russian |
| `/deleteaccount` | Delete all your data |
| `/cancel` | Cancel the current operation |

---

## 📁 Project Structure

```
src/main/java/com/festiva/
├── 🤖 bot/            # BirthdayBot, CallbackQueryHandler, sub-handlers
├── 💬 command/
│   └── handler/       # All command handlers (Start, Add, Edit, …)
├── 👥 friend/
│   ├── api/           # FriendService interface
│   ├── entity/        # Friend, Relationship
│   └── repository/    # FriendMongoRepository
├── 🌐 i18n/           # Lang enum, Messages (EN + RU)
├── 📊 metrics/        # Kafka metrics sender
├── 🔔 notification/   # BirthdayReminder scheduler
├── 🗂️ state/          # UserStateService, BotState
└── 👤 user/           # UserPreference, UserPreferenceRepository
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

## 📖 Feature Documentation

Each feature has a full spec covering user stories, functional & non-functional requirements, bot flows, state transitions, error messages, acceptance criteria, data model, security, observability, and known limitations.

| Feature | Spec |
|---|---|
| Add Friend | [docs/features/add-friend.md](docs/features/add-friend.md) |
| Bulk Add Friends | [docs/features/bulk-add.md](docs/features/bulk-add.md) |
| Edit Friend | [docs/features/edit-friend.md](docs/features/edit-friend.md) |
| Remove Friend | [docs/features/remove-friend.md](docs/features/remove-friend.md) |
| List Friends | [docs/features/list.md](docs/features/list.md) |
| Search Friends | [docs/features/search.md](docs/features/search.md) |
| Export Friends | [docs/features/export.md](docs/features/export.md) |
| Birthdays by Month | [docs/features/birthdays-by-month.md](docs/features/birthdays-by-month.md) |
| Today's Birthdays | [docs/features/today.md](docs/features/today.md) |
| Upcoming Birthdays | [docs/features/upcoming-birthdays.md](docs/features/upcoming-birthdays.md) |
| Jubilee Birthdays | [docs/features/jubilee.md](docs/features/jubilee.md) |
| Birthday Reminders | [docs/features/birthday-reminders.md](docs/features/birthday-reminders.md) |
| Stats | [docs/features/stats.md](docs/features/stats.md) |
| Settings | [docs/features/settings.md](docs/features/settings.md) |
| Language | [docs/features/language.md](docs/features/language.md) |
| Delete Account | [docs/features/delete-account.md](docs/features/delete-account.md) |


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
