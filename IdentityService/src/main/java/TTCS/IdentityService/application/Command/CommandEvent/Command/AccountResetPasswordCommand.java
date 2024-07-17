package TTCS.IdentityService.application.Command.CommandEvent.Command;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Date;

@Getter
@ToString
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class AccountResetPasswordCommand {
    @TargetAggregateIdentifier
    String idAccount;
    int otp;
    String newPassword;
    Date executeAt;

}
