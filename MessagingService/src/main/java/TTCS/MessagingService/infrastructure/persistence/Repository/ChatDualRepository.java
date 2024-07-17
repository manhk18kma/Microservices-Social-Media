package TTCS.MessagingService.infrastructure.persistence.Repository;

import TTCS.MessagingService.Domain.Model.ChatDual;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatDualRepository   extends MongoRepository<ChatDual, String> {
    ChatDual findByIdChatProfile1AndIdChatProfile2OrIdChatProfile2AndIdChatProfile1(
            String id1,String i2,String id3,String id4
    );
}
