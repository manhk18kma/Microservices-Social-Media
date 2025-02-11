package TTCS.MessagingService.Presentation.DTO.Response;

import TTCS.MessagingService.Domain.Model.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
public class ContactsResponse {
     String idChat;
     String chatName;
     Date lastUsed;
     Boolean isChecked;
     String urlImageChatRoom;
     String lastMessage;
     Status status;
     String idProfile;

}
