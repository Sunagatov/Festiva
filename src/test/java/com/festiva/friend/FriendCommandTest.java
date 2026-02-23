package com.festiva.friend;

import com.festiva.IntegrationTestBase;
import com.festiva.command.CommandRouter;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FriendCommandTest extends IntegrationTestBase {

    @Autowired
    CommandRouter commandRouter;

    @Autowired
    FriendService friendService;

    @Autowired
    UserMongoRepository userMongoRepository;

    @BeforeEach
    void clean() {
        userMongoRepository.deleteAll();
    }

    @Test
    void addFriend_thenListShowsFriend() {
        SendMessage addPrompt = commandRouter.route(update(1L, 1L, "/add"));
        assertThat(addPrompt.getText()).contains("Введите имя");

        SendMessage addResult = commandRouter.route(update(1L, 1L, "Alice 1990-06-15"));
        assertThat(addResult.getText()).contains("успешно добавлен");

        List<Friend> friends = friendService.getFriendsSortedByDayMonth(1L);
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getName()).isEqualTo("Alice");
        assertThat(friends.get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    void addDuplicateFriend_returnsError() {
        commandRouter.route(update(2L, 2L, "/add"));
        commandRouter.route(update(2L, 2L, "Bob 1985-03-20"));

        commandRouter.route(update(2L, 2L, "/add"));
        SendMessage duplicate = commandRouter.route(update(2L, 2L, "Bob 1985-03-20"));
        assertThat(duplicate.getText()).contains("уже существует");
    }

    @Test
    void removeFriend_thenListIsEmpty() {
        commandRouter.route(update(3L, 3L, "/add"));
        commandRouter.route(update(3L, 3L, "Carol 2000-01-01"));

        commandRouter.route(update(3L, 3L, "/remove"));
        SendMessage removeResult = commandRouter.route(update(3L, 3L, "Carol"));
        assertThat(removeResult.getText()).contains("успешно удалён");

        assertThat(friendService.getFriendsSortedByDayMonth(3L)).isEmpty();
    }

    @Test
    void addFriend_invalidDate_returnsError() {
        commandRouter.route(update(4L, 4L, "/add"));
        SendMessage result = commandRouter.route(update(4L, 4L, "Dave not-a-date"));
        assertThat(result.getText()).contains("Неверный формат даты");
    }

    @Test
    void addFriend_futureBirthDate_returnsError() {
        commandRouter.route(update(5L, 5L, "/add"));
        SendMessage result = commandRouter.route(update(5L, 5L, "Eve 2099-01-01"));
        assertThat(result.getText()).contains("не может быть в будущем");
    }

    @Test
    void cancelDuringAdd_clearsState() {
        commandRouter.route(update(6L, 6L, "/add"));
        SendMessage cancelResult = commandRouter.route(update(6L, 6L, "/cancel"));
        assertThat(cancelResult.getText()).contains("отменена");

        assertThat(friendService.getFriendsSortedByDayMonth(6L)).isEmpty();
    }

    private Update update(long userId, long chatId, String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getText()).thenReturn(text);
        when(message.hasText()).thenReturn(true);

        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
