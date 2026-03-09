package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("DatePickerCallbackHandler")
@ExtendWith(MockitoExtension.class)
class DatePickerCallbackHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks DatePickerCallbackHandler handler;

    @BeforeEach
    void setup() {
        lenient().when(userStateService.getPendingName(anyLong())).thenReturn("Alice");
        lenient().when(userStateService.getPendingYear(anyLong())).thenReturn(1990);
        lenient().when(userStateService.getPendingMonth(anyLong())).thenReturn(3);
        lenient().when(userStateService.getYearPageOffset(anyLong())).thenReturn(DatePickerKeyboard.DEFAULT_YEAR_OFFSET);
    }

    @Test
    @DisplayName("handleYearPage with null pendingName → SESSION_EXPIRED")
    void handleYearPage_nullName_returnsSessionExpired() {
        when(userStateService.getPendingName(1L)).thenReturn(null);

        CallbackResult result = handler.handleYearPage(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX + "0", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleYearPick with null pendingName → SESSION_EXPIRED")
    void handleYearPick_nullName_returnsSessionExpired() {
        when(userStateService.getPendingName(1L)).thenReturn(null);

        CallbackResult result = handler.handleYearPick(DatePickerKeyboard.DATE_YEAR_PREFIX + "1990", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleMonthPick with null pendingName → SESSION_EXPIRED")
    void handleMonthPick_nullName_returnsSessionExpired() {
        when(userStateService.getPendingName(1L)).thenReturn(null);

        CallbackResult result = handler.handleMonthPick(DatePickerKeyboard.DATE_MONTH_PREFIX + "3", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleDayPick future date → error text AND keyboard returned")
    void handleDayPick_futureDate_returnsErrorWithKeyboard() {
        LocalDate future = LocalDate.now().plusYears(1);
        when(userStateService.getPendingYear(1L)).thenReturn(future.getYear());
        when(userStateService.getPendingMonth(1L)).thenReturn(future.getMonthValue());

        CallbackResult result = handler.handleDayPick(
                DatePickerKeyboard.DATE_DAY_PREFIX + future.getDayOfMonth(), 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.DATE_FUTURE_ERROR));
        assertThat(result.markup).isNotNull();
    }

    @Test
    @DisplayName("handleRelationship at cap → clears state and returns cap message")
    void handleRelationship_atCap_clearsStateAndReturnsCap() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(
                java.util.Collections.nCopies(FriendService.FRIEND_CAP, null));

        CallbackResult result = handler.handleRelationship(
                DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "FRIEND", 1L, Lang.EN);

        verify(userStateService).clearState(1L);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.FRIEND_CAP, FriendService.FRIEND_CAP));
    }

    @Test
    @DisplayName("handleYearPick prompt contains /cancel hint")
    void handleYearPick_prompt_containsCancelHint() {
        CallbackResult result = handler.handleYearPick(DatePickerKeyboard.DATE_YEAR_PREFIX + "1990", 1L, Lang.EN);

        assertThat(result.text).contains("/cancel");
    }

    @Test
    @DisplayName("handleRelationship prompt contains /cancel hint")
    void handleRelationship_prompt_containsCancelHint() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        CallbackResult result = handler.handleRelationship(
                DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "SKIP", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.FRIEND_ADDED, "Alice"));
    }

    @Test
    @DisplayName("handleRelationship RU → returns RU success message")
    void handleRelationship_ru_returnsRuMessage() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        CallbackResult result = handler.handleRelationship(
                DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "SKIP", 1L, Lang.RU);

        assertThat(result.text).contains(Messages.get(Lang.RU, Messages.FRIEND_ADDED, "Alice"));
    }

    @Test
    @DisplayName("handleDayPick add flow → relationship prompt contains /cancel hint")
    void handleDayPick_addFlow_relationshipPromptContainsCancelHint() {
        when(userStateService.getState(1L)).thenReturn(BotState.WAITING_FOR_ADD_FRIEND_DATE);

        CallbackResult result = handler.handleDayPick(DatePickerKeyboard.DATE_DAY_PREFIX + "15", 1L, Lang.EN);

        assertThat(result.text).contains("/cancel");
    }

    @Test
    @DisplayName("handleDayPick add flow success → friend_added message contains next-step hint")
    void handleRelationship_success_containsNextStepHint() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        CallbackResult result = handler.handleRelationship(
                DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "SKIP", 1L, Lang.EN);

        assertThat(result.markup).isNotNull();
    }

    @Test
    @DisplayName("handleDayPick in add flow → transitions to relationship picker")
    void handleDayPick_addFlow_showsRelationshipPicker() {
        when(userStateService.getState(1L)).thenReturn(BotState.WAITING_FOR_ADD_FRIEND_DATE);

        CallbackResult result = handler.handleDayPick(DatePickerKeyboard.DATE_DAY_PREFIX + "15", 1L, Lang.EN);

        verify(userStateService).setState(1L, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.RELATIONSHIP_PICK, "Alice"));
        assertThat(result.markup).isNotNull();
    }

    @Test
    @DisplayName("handleDayPick in edit flow → updates date and clears state")
    void handleDayPick_editFlow_updatesDate() {
        when(userStateService.getState(1L)).thenReturn(BotState.WAITING_FOR_EDIT_DATE);

        CallbackResult result = handler.handleDayPick(DatePickerKeyboard.DATE_DAY_PREFIX + "15", 1L, Lang.EN);

        verify(friendService).updateFriendDate(eq(1L), eq("Alice"), eq(LocalDate.of(1990, 3, 15)));
        verify(userStateService).clearState(1L);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.EDIT_DATE_DONE, "Alice"));
    }

    @Test
    @DisplayName("handleDayPick with future date → returns date-future error")
    void handleDayPick_futureDate_returnsError() {
        LocalDate future = LocalDate.now().plusYears(1);
        when(userStateService.getPendingYear(1L)).thenReturn(future.getYear());
        when(userStateService.getPendingMonth(1L)).thenReturn(future.getMonthValue());

        CallbackResult result = handler.handleDayPick(
                DatePickerKeyboard.DATE_DAY_PREFIX + future.getDayOfMonth(), 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.DATE_FUTURE_ERROR));
    }

    @Test
    @DisplayName("handleDayPick with null session state → returns SESSION_EXPIRED")
    void handleDayPick_nullSession_returnsSessionExpired() {
        when(userStateService.getPendingYear(1L)).thenReturn(null);

        CallbackResult result = handler.handleDayPick(DatePickerKeyboard.DATE_DAY_PREFIX + "15", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleRelationship → adds friend with relationship and clears state")
    void handleRelationship_addsFriendWithRelationship() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        CallbackResult result = handler.handleRelationship(
                DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "FRIEND", 1L, Lang.EN);

        verify(friendService).addFriend(eq(1L), argThat(f ->
                "Alice".equals(f.getName()) && f.getRelationship() == Relationship.FRIEND));
        verify(userStateService).clearState(1L);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.FRIEND_ADDED, "Alice"));
    }

    @Test
    @DisplayName("handleRelationship with SKIP → adds friend with null relationship")
    void handleRelationship_skip_addsWithNullRelationship() {
        when(userStateService.getPendingDay(1L)).thenReturn(15);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        handler.handleRelationship(DatePickerCallbackHandler.RELATIONSHIP_PREFIX + "SKIP", 1L, Lang.EN);

        verify(friendService).addFriend(eq(1L), argThat(f -> f.getRelationship() == null));
    }

    @Test
    @DisplayName("handleEditRelationship with pendingId → looks up by ID and updates")
    void handleEditRelationship_withPendingId_updatesByFriend() {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 3, 15));
        alice.setId("id-alice");
        when(userStateService.getPendingId(1L)).thenReturn("id-alice");
        when(friendService.findFriendById("id-alice")).thenReturn(Optional.of(alice));

        CallbackResult result = handler.handleEditRelationship(
                DatePickerCallbackHandler.EDIT_REL_PREFIX + "FRIEND", 1L, Lang.EN);

        verify(friendService).updateFriendRelationship(1L, "Alice", Relationship.FRIEND);
        verify(userStateService).clearState(1L);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.EDIT_REL_DONE, "Alice"));
    }

    @Test
    @DisplayName("handleEditRelationship with SKIP → sets relationship to null")
    void handleEditRelationship_skip_setsNull() {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 3, 15));
        alice.setId("id-alice");
        when(userStateService.getPendingId(1L)).thenReturn("id-alice");
        when(friendService.findFriendById("id-alice")).thenReturn(Optional.of(alice));

        handler.handleEditRelationship(DatePickerCallbackHandler.EDIT_REL_PREFIX + "SKIP", 1L, Lang.EN);

        verify(friendService).updateFriendRelationship(1L, "Alice", null);
    }
}
