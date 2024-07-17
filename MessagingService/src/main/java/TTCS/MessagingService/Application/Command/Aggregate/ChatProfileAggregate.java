package TTCS.MessagingService.Application.Command.Aggregate;

import KMA.TTCS.CommonService.command.AccountProfileCommand.ChatProfileCreateCommand;
import KMA.TTCS.CommonService.command.AccountProfileCommand.ChatProfileRollbackCommand;
import KMA.TTCS.CommonService.command.IdentityMessage.ConnectChatProfileCommand;
import KMA.TTCS.CommonService.command.IdentityMessage.DisConnectChatCommand;
import KMA.TTCS.CommonService.event.AccountProfile.ChatProfileCreateEvent;
import KMA.TTCS.CommonService.event.AccountProfile.ChatProfileRollbackEvent;
import KMA.TTCS.CommonService.event.IdentityMessage.ConnectChatProfileEvent;
import KMA.TTCS.CommonService.event.IdentityMessage.DisConnectChatEvent;
import TTCS.MessagingService.Domain.Model.Status;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Slf4j
@Aggregate
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatProfileAggregate {
    @AggregateIdentifier
    String idChatProfile;
    String idProfile;
    Status status;

    public ChatProfileAggregate() {
    }

    @CommandHandler
    public ChatProfileAggregate(ChatProfileCreateCommand command) {
        log.info("Handling ChatProfileCreateCommand: {}", command);
        ChatProfileCreateEvent event = new ChatProfileCreateEvent(
                command.getIdChatProfile(),
                command.getIdProfile(),
                command.getIdAccount(),
                command.getEmail()
        );
        log.info("Applying ChatProfileCreateEvent: {}", event);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(ChatProfileCreateEvent event) {
        log.info("Handling ChatProfileCreateEvent: {}", event);
        this.idChatProfile = event.getIdChatProfile();
        this.idProfile = event.getIdProfile();
    }

    @CommandHandler
    public void handle(ChatProfileRollbackCommand command) {
        log.info("Handling ChatProfileRollbackCommand: {}", command);
        ChatProfileRollbackEvent event = new ChatProfileRollbackEvent(command.getIdChatProfile());
        log.info("Applying ChatProfileRollbackEvent: {}", event);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(ChatProfileRollbackEvent event) {
        log.info("Handling ChatProfileRollbackEvent: {}", event);
        this.idChatProfile = event.getIdChatProfile();
    }

    @CommandHandler
    public void handle(ConnectChatProfileCommand command) {
        log.info("Handling ConnectChatProfileCommand: {}", command);
        ConnectChatProfileEvent event = new ConnectChatProfileEvent(
                command.getIdChatProfile(),
                Status.ONLINE.toString()
        );
        log.info("Applying ConnectChatProfileEvent: {}", event);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(ConnectChatProfileEvent event) {
        log.info("Handling ConnectChatProfileEvent: {}", event);
        this.idChatProfile = event.getIdChatProfile();
        this.status = Status.valueOf(event.getStatus());
    }

    @CommandHandler
    public void handle(DisConnectChatCommand command) {
        log.info("Handling DisConnectChatCommand: {}", command);
        DisConnectChatEvent event = new DisConnectChatEvent(
                command.getIdChatProfile(),
                Status.OFFLINE.toString()
        );
        log.info("Applying DisConnectChatEvent: {}", event);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(DisConnectChatEvent event) {
        log.info("Handling DisConnectChatEvent: {}", event);
        this.idChatProfile = event.getIdChatProfile();
        this.status = Status.valueOf(event.getStatus());
    }
}
