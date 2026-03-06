package com.festiva.notification;

import com.festiva.IntegrationTestBase;
import com.festiva.bot.BirthdayBot;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BirthdayReminderTest extends IntegrationTestBase {

    // Default lang for new users is RU — tests use the same default
    private static final Lang DEFAULT_LANG = Lang.RU;

    @Autowired
    BirthdayReminder birthdayReminder;

    @Autowired
    FriendService friendService;

    @Autowired
    FriendMongoRepository friendMongoRepository;

    @Autowired
    BirthdayBot birthdayBot;

    @BeforeEach
    void clean() {
        friendMongoRepository.deleteAll();
    }

    @Test
    void checkBirthdays_todayBirthday_sendsNotification() {
        friendService.addFriend(10L, new Friend("Today Person", LocalDate.now().minusYears(30)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(10L), contains(
                Messages.get(DEFAULT_LANG, Messages.NOTIFY_TODAY, "").substring(0, 5)));
    }

    @Test
    void checkBirthdays_tomorrowBirthday_sendsNotification() {
        friendService.addFriend(11L, new Friend("Tomorrow Person", LocalDate.now().plusDays(1).minusYears(25)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(11L), contains(
                Messages.get(DEFAULT_LANG, Messages.NOTIFY_TOMORROW, "").substring(0, 5)));
    }

    @Test
    void checkBirthdays_inOneWeek_sendsNotification() {
        friendService.addFriend(12L, new Friend("Week Person", LocalDate.now().plusDays(7).minusYears(20)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(12L), contains(
                Messages.get(DEFAULT_LANG, Messages.NOTIFY_WEEK, "").substring(0, 5)));
    }

    @Test
    void checkBirthdays_notRelevantDate_noNotification() {
        friendService.addFriend(13L, new Friend("Far Person", LocalDate.now().plusDays(10).minusYears(20)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot, never()).send(anyLong(), anyString());
    }
}
