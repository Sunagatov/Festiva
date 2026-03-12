package com.festiva.bot;

import com.festiva.i18n.Lang;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

@Component
public class BotCommandsProvider {

    public List<BotCommand> getCommandsForLanguage(Lang lang) {
        if (lang == Lang.RU) {
            return List.of(
                    new BotCommand("start",             "Запустить"),
                    new BotCommand("add",               "Добавить друга"),
                    new BotCommand("addmany",           "Добавить несколько"),
                    new BotCommand("list",              "Список друзей"),
                    new BotCommand("edit",              "Редактировать друга"),
                    new BotCommand("remove",            "Удалить друга"),
                    new BotCommand("search",            "Поиск друзей"),
                    new BotCommand("birthdays",         "Дни рождения по месяцам"),
                    new BotCommand("today",             "Сегодняшние дни рождения"),
                    new BotCommand("upcomingbirthdays", "Ближайшие дни рождения"),
                    new BotCommand("jubilee",           "Юбилейные дни рождения"),
                    new BotCommand("stats",             "Статистика"),
                    new BotCommand("export",            "Экспорт друзей"),
                    new BotCommand("settings",          "Настройки"),
                    new BotCommand("language",          "Сменить язык"),
                    new BotCommand("menu",              "Все команды"),
                    new BotCommand("about",             "О боте"),
                    new BotCommand("deleteaccount",     "Удалить данные"),
                    new BotCommand("cancel",            "Отмена"),
                    new BotCommand("importics",         "Импорт из Google Календаря")
            );
        } else {
            return List.of(
                    new BotCommand("start",             "Start"),
                    new BotCommand("add",               "Add friend"),
                    new BotCommand("addmany",           "Bulk add friends"),
                    new BotCommand("list",              "List friends"),
                    new BotCommand("edit",              "Edit friend"),
                    new BotCommand("remove",            "Remove friend"),
                    new BotCommand("search",            "Search friends"),
                    new BotCommand("birthdays",         "Birthdays by month"),
                    new BotCommand("today",             "Today's birthdays"),
                    new BotCommand("upcomingbirthdays", "Upcoming birthdays"),
                    new BotCommand("jubilee",           "Milestone birthdays"),
                    new BotCommand("stats",             "Statistics"),
                    new BotCommand("export",            "Export friends"),
                    new BotCommand("settings",          "Settings"),
                    new BotCommand("language",          "Change language"),
                    new BotCommand("menu",              "Show all commands"),
                    new BotCommand("about",             "About Festiva"),
                    new BotCommand("deleteaccount",     "Delete my data"),
                    new BotCommand("cancel",            "Cancel"),
                    new BotCommand("importics",         "Import from Google Calendar")
            );
        }
    }
}
