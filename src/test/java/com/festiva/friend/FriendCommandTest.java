package com.festiva.friend;

import com.festiva.IntegrationTestBase;
import com.festiva.command.CommandRouter;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
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

    private static final Lang L = Lang.RU;

    @Autowired
    CommandRouter commandRouter;

    @Autowired
    FriendService friendService;

    @Autowired
    FriendMongoRepository friendMongoRepository;

    @BeforeEach
    void clean() {
        friendMongoRepository.deleteAll();
    }

    @Test
    void addFriend_thenListShowsFriend() {
        SendMessage namePrompt = commandRouter.route(update(1L, 1L, "/add"));
        assertThat(namePrompt.getText()).contains(Messages.get(L, Messages.ENTER_NAME).substring(0, 5));

        SendMessage datePrompt = commandRouter.route(update(1L, 1L, "Alice"));
        assertThat(datePrompt.getText()).contains(Messages.get(L, Messages.ENTER_DATE, "Alice").substring(0, 5));

        SendMessage addResult = commandRouter.route(update(1L, 1L, "15.06.1990"));
        assertThat(addResult.getText()).contains(Messages.get(L, Messages.FRIEND_ADDED, "Alice").substring(0, 2));

        List<Friend> friends = friendService.getFriendsSortedByDayMonth(1L);
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getName()).isEqualTo("Alice");
        assertThat(friends.get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    void addDuplicateFriend_returnsError() {
        commandRouter.route(update(2L, 2L, "/add"));
        commandRouter.route(update(2L, 2L, "Bob"));
        commandRouter.route(update(2L, 2L, "20.03.1985"));

        commandRouter.route(update(2L, 2L, "/add"));
        SendMessage duplicate = commandRouter.route(update(2L, 2L, "Bob"));
        assertThat(duplicate.getText()).contains(Messages.get(L, Messages.NAME_EXISTS, "Bob").substring(0, 5));
    }

    @Test
    void removeFriend_thenListIsEmpty() {
        commandRouter.route(update(3L, 3L, "/add"));
        commandRouter.route(update(3L, 3L, "Carol"));
        commandRouter.route(update(3L, 3L, "01.01.2000"));

        commandRouter.route(update(3L, 3L, "/remove"));
        SendMessage removeResult = commandRouter.route(update(3L, 3L, "Carol"));
        assertThat(removeResult.getText()).contains(Messages.get(L, Messages.FRIEND_REMOVED, "Carol").substring(0, 2));

        assertThat(friendService.getFriendsSortedByDayMonth(3L)).isEmpty();
    }

    @Test
    void addFriend_invalidDate_returnsError() {
        commandRouter.route(update(4L, 4L, "/add"));
        commandRouter.route(update(4L, 4L, "Dave"));
        SendMessage result = commandRouter.route(update(4L, 4L, "not-a-date"));
        assertThat(result.getText()).contains(Messages.get(L, Messages.DATE_FORMAT_ERROR).substring(0, 5));
    }

    @Test
    void addFriend_futureBirthDate_returnsError() {
        commandRouter.route(update(5L, 5L, "/add"));
        commandRouter.route(update(5L, 5L, "Eve"));
        SendMessage result = commandRouter.route(update(5L, 5L, "01.01.2099"));
        assertThat(result.getText()).contains(Messages.get(L, Messages.DATE_FUTURE_ERROR).substring(0, 5));
    }

    @Test
    void cancelDuringAdd_clearsState() {
        commandRouter.route(update(6L, 6L, "/add"));
        SendMessage cancelResult = commandRouter.route(update(6L, 6L, "/cancel"));
        assertThat(cancelResult.getText()).contains(Messages.get(L, Messages.CANCEL_ACTIVE).substring(0, 5));

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
