package com.festiva.friend;

import com.festiva.IntegrationTestBase;
import com.festiva.command.CommandRouter;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Friend commands (integration)")
class FriendCommandTest extends IntegrationTestBase {

    private static final Lang L = Lang.RU;

    @Autowired CommandRouter commandRouter;
    @Autowired FriendService friendService;
    @Autowired FriendMongoRepository friendMongoRepository;

    @BeforeEach
    void clean() { friendMongoRepository.deleteAll(); }

    @Test
    @DisplayName("/add → name → date persists friend and confirms")
    void addFriend_persistsAndConfirms() {
        commandRouter.route(update(1L, "/add"));
        commandRouter.route(update(1L, "Alice"));
        var result = commandRouter.route(update(1L, "15.06.1990"));

        assertThat(result.getText()).contains("Alice");

        Friend saved = friendService.getFriends(1L).getFirst();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    @DisplayName("/add duplicate name → returns name-exists error containing the name")
    void addDuplicateFriend_returnsError() {
        commandRouter.route(update(2L, "/add"));
        commandRouter.route(update(2L, "Bob"));
        commandRouter.route(update(2L, "20.03.1985"));

        commandRouter.route(update(2L, "/add"));
        var result = commandRouter.route(update(2L, "Bob"));

        assertThat(result.getText()).contains(Messages.get(L, Messages.NAME_EXISTS, "Bob"));
    }

    @Test
    @DisplayName("/remove existing friend → confirms removal, list is empty")
    void removeFriend_confirmsAndListIsEmpty() {
        commandRouter.route(update(3L, "/add"));
        commandRouter.route(update(3L, "Carol"));
        commandRouter.route(update(3L, "01.01.2000"));

        commandRouter.route(update(3L, "/remove"));
        var result = commandRouter.route(update(3L, "Carol"));

        assertThat(result.getText()).contains("Carol");
        assertThat(friendService.getFriends(3L)).isEmpty();
    }

    @Test
    @DisplayName("/remove non-existent friend via text state → returns not-found message")
    void removeFriend_notFound_returnsNotFound() {
        friendService.addFriend(8L, new Friend("Real", LocalDate.of(1990, 1, 1)));
        commandRouter.route(update(8L, "/remove"));
        var result = commandRouter.route(update(8L, "Ghost"));
        assertThat(result.getText()).contains(Messages.get(L, Messages.FRIEND_NOT_FOUND, "Ghost"));
    }

    @Test
    @DisplayName("/add with unparseable date → returns date-format error")
    void addFriend_invalidDate_returnsError() {
        commandRouter.route(update(4L, "/add"));
        commandRouter.route(update(4L, "Dave"));
        var result = commandRouter.route(update(4L, "not-a-date"));

        assertThat(result.getText()).contains(Messages.get(L, Messages.DATE_FORMAT_ERROR));
    }

    @Test
    @DisplayName("/add with future birth date → returns future-date error")
    void addFriend_futureBirthDate_returnsError() {
        commandRouter.route(update(5L, "/add"));
        commandRouter.route(update(5L, "Eve"));
        var result = commandRouter.route(update(5L, "01.01.2099"));

        assertThat(result.getText()).contains(Messages.get(L, Messages.DATE_FUTURE_ERROR));
    }

    @Test
    @DisplayName("/cancel during /add flow → confirms cancel, no friend saved")
    void cancelDuringAdd_clearsState() {
        commandRouter.route(update(6L, "/add"));
        var result = commandRouter.route(update(6L, "/cancel"));

        assertThat(result.getText()).contains(Messages.get(L, Messages.CANCEL_ACTIVE));
        assertThat(friendService.getFriends(6L)).isEmpty();
    }

    private Update update(long userId, String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(userId);
        when(message.getText()).thenReturn(text);
        when(message.hasText()).thenReturn(true);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
