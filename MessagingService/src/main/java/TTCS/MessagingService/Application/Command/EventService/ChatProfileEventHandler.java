package TTCS.MessagingService.Application.Command.EventService;

import KMA.TTCS.CommonService.event.AccountProfile.ChatProfileCreateEvent;
import KMA.TTCS.CommonService.event.AccountProfile.ChatProfileRollbackEvent;
import KMA.TTCS.CommonService.event.IdentityMessage.ConnectChatProfileEvent;
import KMA.TTCS.CommonService.event.IdentityMessage.DisConnectChatEvent;
import KMA.TTCS.CommonService.model.GetIdChatProfileResponse;
import KMA.TTCS.CommonService.model.GetIdChatResponse;
import KMA.TTCS.CommonService.query.GetIdChatProfileQuery;
import KMA.TTCS.CommonService.query.GetIdChatQuery;
import TTCS.MessagingService.Domain.Model.ChatDual;
import TTCS.MessagingService.Domain.Model.ChatProfile;
import TTCS.MessagingService.Domain.Model.Status;
import TTCS.MessagingService.infrastructure.persistence.Repository.ChatDualRepository;
import TTCS.MessagingService.infrastructure.persistence.Repository.ProfileChatRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@RequiredArgsConstructor
public class ChatProfileEventHandler {
    final ProfileChatRepository profileChatRepository;
    final ChatDualRepository chatDualRepository;

    @EventHandler
    public void on(ChatProfileCreateEvent event){
        ChatProfile chatProfile = ChatProfile.builder()
                .idChatProfile(event.getIdChatProfile())
                .idProfile(event.getIdProfile())
                .status(Status.OFFLINE)
                .chatRoomLastUsed(new HashMap<>())
                .chatRoomChecked(new HashMap<>())
                .build();
        profileChatRepository.save(chatProfile);
    }

    @EventHandler
    public void on(ChatProfileRollbackEvent event){
        profileChatRepository.deleteById(event.getIdChatProfile());
    }
    @EventHandler
    public void on(ConnectChatProfileEvent event){
        ChatProfile chatProfile = profileChatRepository.findById(event.getIdChatProfile()).get();
        chatProfile.setStatus(Status.valueOf(event.getStatus()));
        chatProfile.setStatus(Status.ONLINE);

        profileChatRepository.save(chatProfile);

    }

    @EventHandler
    public void on(DisConnectChatEvent event){
        ChatProfile chatProfile = profileChatRepository.findById(event.getIdChatProfile()).get();
        chatProfile.setStatus(Status.valueOf(event.getStatus()));
        chatProfile.setStatus(Status.OFFLINE);

        profileChatRepository.save(chatProfile);;

    }



    @QueryHandler
    public GetIdChatProfileResponse handle(GetIdChatProfileQuery query){
        return new GetIdChatProfileResponse(
                profileChatRepository.findByIdProfile(query.getIdProfile()).getIdChatProfile()
        );
    }

    @QueryHandler
    public GetIdChatResponse handle(GetIdChatQuery query){
        ChatProfile chatProfile1= profileChatRepository.findByIdProfile(query.getIdProfile1());
        ChatProfile chatProfile2 = profileChatRepository.findByIdProfile(query.getIdProfile2());


        ChatDual chatDual = chatDualRepository.findByIdChatProfile1AndIdChatProfile2OrIdChatProfile2AndIdChatProfile1(
                chatProfile1.getIdChatProfile(),chatProfile2.getIdChatProfile(),
                chatProfile2.getIdChatProfile(),chatProfile1.getIdChatProfile()
        );
        String idChat = chatDual==null ? null : chatDual.getIdChatDual();
        return new GetIdChatResponse(idChat);
    }

}
