package com.festiva.friend;

import com.festiva.IntegrationTestBase;
import com.festiva.bot.CallbackQueryHandler;
import com.festiva.command.CommandRouter;
import com.festiva.command.DatePickerKeyboard;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Friend commands (integration)")
class FriendCommandTest extends IntegrationTestBase {

    private static final Lang L = Lang.RU;

    @Autowired CommandRouter commandRouter;
    @Autowired CallbackQueryHandler callbackQueryHandler;
    @Autowired FriendService friendService;
    @Autowired FriendMongoRepository friendMongoRepository;

    @BeforeEach
    void clean() { friendMongoRepository.deleteAll(); }

    @Test
    @DisplayName("/add → name → year/month/day callbacks → persists friend and confirms")
    void addFriend_persistsAndConfirms() {
        commandRouter.route(update(1L, "/add"));
        commandRouter.route(update(1L, "Alice"));
        callbackQueryHandler.handle(callback(1L, DatePickerKeyboard.DATE_YEAR_PREFIX + "1990"));
        callbackQueryHandler.handle(callback(1L, DatePickerKeyboard.DATE_MONTH_PREFIX + "6"));
        callbackQueryHandler.handle(callback(1L, DatePickerKeyboard.DATE_DAY_PREFIX + "15"));
        callbackQueryHandler.handle(callback(1L, "RELATIONSHIP_FRIEND"));

        Friend saved = friendService.getFriends(1L).getFirst();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    @DisplayName("/add duplicate name → returns name-exists error containing the name")
    void addDuplicateFriend_returnsError() {
        friendService.addFriend(2L, new Friend("Bob", LocalDate.of(1985, 3, 20)));

        commandRouter.route(update(2L, "/add"));
        var result = commandRouter.route(update(2L, "Bob"));

        assertThat(result.getText()).contains(Messages.get(L, Messages.NAME_EXISTS, "Bob"));
    }

    @Test
    @DisplayName("/remove existing friend → confirms removal, list is empty")
    void removeFriend_confirmsAndListIsEmpty() {
        friendService.addFriend(3L, new Friend("Carol", LocalDate.of(2000, 1, 1)));
        String friendId = friendService.getFriends(3L).getFirst().getId();

        commandRouter.route(update(3L, "/remove"));
        callbackQueryHandler.handle(callback(3L, "REMOVE_" + friendId));
        callbackQueryHandler.handle(callback(3L, "CONFIRM_REMOVE_" + friendId));

        assertThat(friendService.getFriends(3L)).isEmpty();
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

    private CallbackQuery callback(long userId, String data) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        MaybeInaccessibleMessage message = mock(MaybeInaccessibleMessage.class);
        when(message.getChatId()).thenReturn(userId);
        when(message.getMessageId()).thenReturn(1);
        CallbackQuery cq = mock(CallbackQuery.class);
        when(cq.getFrom()).thenReturn(user);
        when(cq.getData()).thenReturn(data);
        when(cq.getMessage()).thenReturn(message);
        return cq;
    }
}
