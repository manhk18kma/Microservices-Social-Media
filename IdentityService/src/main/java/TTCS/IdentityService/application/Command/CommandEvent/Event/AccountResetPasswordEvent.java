package TTCS.IdentityService.application.Command.CommandEvent.Event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AccountResetPasswordEvent {
    String idAccount;
    String newPassword;

}
