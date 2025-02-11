package TTCS.IdentityService.application.Command.CommandService;


import KMA.TTCS.CommonService.*;
import KMA.TTCS.CommonService.command.AccountProfileCommand.AccountGenerateOTPCommand;
import KMA.TTCS.CommonService.command.IdentityMessage.ConnectChatProfileCommand;
import KMA.TTCS.CommonService.command.IdentityMessage.DisConnectChatCommand;
import TTCS.IdentityService.application.Command.Aggregate.FutureTracker;
import TTCS.IdentityService.application.Command.CommandEvent.Command.AccountActiveCommand;
import TTCS.IdentityService.application.Command.CommandEvent.Command.AccountChangePasswordCommand;
import TTCS.IdentityService.application.Command.CommandEvent.Command.AccountCreateCommand;
import TTCS.IdentityService.application.Command.CommandEvent.Command.AccountResetPasswordCommand;
import TTCS.IdentityService.application.Command.CommandService.DTO.AccountCommandResponse;
import TTCS.IdentityService.application.Command.CommandService.DTO.OTPResponse;
import TTCS.IdentityService.application.Exception.AppException.AppErrorCode;
import TTCS.IdentityService.application.Exception.AppException.AppException;
import TTCS.IdentityService.domain.enumType.UserStatus;
import TTCS.IdentityService.domain.model.Account;
import TTCS.IdentityService.domain.model.Role;
import TTCS.IdentityService.infrastructure.persistence.repository.AccountRepository;
import TTCS.IdentityService.infrastructure.persistence.repository.RoleRepository;
import TTCS.IdentityService.infrastructure.persistence.service.DTO.Request.ForgotPasswordReset;
import TTCS.IdentityService.infrastructure.persistence.service.EmailService;
import TTCS.IdentityService.infrastructure.persistence.service.OTPService;
import TTCS.IdentityService.presentation.command.dto.request.AccountActiveRequest;
import TTCS.IdentityService.presentation.command.dto.request.AccountChangePasswordRequest;
import TTCS.IdentityService.presentation.command.dto.request.AccountCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCommandService {
    final CommandGateway commandGateway;
    final AccountRepository accountRepository;
    final OTPService otpService;
    final PasswordEncoder passwordEncoder;
    final RoleRepository roleRepository;
    final EmailService emailService;
    final QueryGateway queryGateway;



    public CompletableFuture<OTPResponse> createAccount(AccountCreateRequest accountCreateRequest) {
        if (accountRepository.existsAccountByEmail(accountCreateRequest.getEmail())) {
            throw new AppException(AppErrorCode.EMAIL_EXISTED);
        }
        if (accountRepository.existsAccountByUsername(accountCreateRequest.getUsername())) {
            throw new AppException(AppErrorCode.USERNAME_EXISTED);
        }
        AccountCreateCommand accountCreateCommand = new AccountCreateCommand();
        BeanUtils.copyProperties(accountCreateRequest, accountCreateCommand);
        accountCreateCommand.setUrlProfilePicture("https://i.pinimg.com/736x/c6/e5/65/c6e56503cfdd87da299f72dc416023d4.jpg");
        accountCreateCommand.setBiography(null);
        String secretKey = otpService.generateSecretKey();
        Account account = Account.builder()
                .idAccount(UUID.randomUUID().toString())
                .username(accountCreateRequest.getUsername())
                .password(passwordEncoder.encode(accountCreateRequest.getPassword()))
                .email(accountCreateRequest.getEmail())
                .createAt(new Date())
                .updateAt(new Date())
                .status(UserStatus.INACTIVE)
                .secretKey(secretKey)
                .roles(new HashSet<>())
                .idProfile(UUID.randomUUID().toString())
                .idChatProfile(UUID.randomUUID().toString())
                .build();
        Role role = roleRepository.findByName("USER");
        account.getRoles().add(role);
        Account account1 = accountRepository.save(account);
        accountCreateCommand.setPassword(account1.getPassword());
        accountCreateCommand.setIdAccount(account.getIdAccount());
        accountCreateCommand.setSecretKey(secretKey);
        accountCreateCommand.setIdProfile(account1.getIdProfile());
        accountCreateCommand.setIdChatProfile(account1.getIdChatProfile());
        accountCreateCommand.setCreateAt(account1.getCreateAt());
        accountCreateCommand.setExecuteAt(new Date());
        accountCreateCommand.setStatus(account1.getStatus());



        CompletableFuture<OTPResponse> future = new CompletableFuture<>();
        FutureTracker.futuresOTP.put(accountCreateCommand.getIdAccount(), future);
        log.info("AccountCreateCommand {}",accountCreateCommand.toString());
        commandGateway.sendAndWait(accountCreateCommand);

        return future.thenApply(result -> {
            return  result;
        });
    }


    public CompletableFuture<AccountCommandResponse> activeAccount(AccountActiveRequest accountActiveRequest, String email) {
        Account account = accountRepository.findByEmail(email);
        if(account==null){
            throw new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED);
        }
        AccountActiveCommand accountActiveCommand = AccountActiveCommand.builder()
                .idAccount(account.getIdAccount())
                .otp(accountActiveRequest.getOtp())
                .status(UserStatus.ACTIVE)
                .executeAt(new Date())
                .build();
        CompletableFuture<String> future = new CompletableFuture<>();
        FutureTracker.futures.put(account.getIdAccount(), future);
        commandGateway.sendAndWait(accountActiveCommand);
        return future.thenApply(result -> {
            AccountCommandResponse accountCommandResponse = new AccountCommandResponse();
            accountCommandResponse.setIdAccount(result);
            return accountCommandResponse;
        });
    }

    @PreAuthorize("#accountChangePasswordRequest.idAccount == authentication.principal.claims['idAccount']  and hasRole('USER')")
    public CompletableFuture<AccountCommandResponse> changePassword(AccountChangePasswordRequest accountChangePasswordRequest) {
        Optional<Account> account = accountRepository.findById(accountChangePasswordRequest.getIdAccount());
        if(!account.isPresent()){
            throw new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED);
        }
        AccountChangePasswordCommand accountChangePasswordCommand = new AccountChangePasswordCommand();
        BeanUtils.copyProperties(accountChangePasswordRequest , accountChangePasswordCommand);
        accountChangePasswordCommand.setExecuteAt(new Date());

        CompletableFuture<String> future = new CompletableFuture<>();
        FutureTracker.futures.put(accountChangePasswordCommand.getIdAccount(), future);
        commandGateway.sendAndWait(accountChangePasswordCommand);
        return future.thenApply(result -> {
            AccountCommandResponse accountCommandResponse = new AccountCommandResponse();
            accountCommandResponse.setIdAccount(result);
            return accountCommandResponse;
        });
    }

//    @PreAuthorize("#id == authentication.principal.claims['idAccount']  and hasRole('USER')")
    public CompletableFuture<OTPResponse> generateOTP(String email) {
        Account account = accountRepository.findByEmail(email);
        if(account==null){
            throw new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED);
        }
        AccountGenerateOTPCommand command = new AccountGenerateOTPCommand();
        command.setIdAccount(account.getIdAccount());
        CompletableFuture<OTPResponse> otpFuture = new CompletableFuture<>();
        FutureTracker.futuresOTP.put(account.getIdAccount(), otpFuture);
        System.out.println(account.getEmail().toString());
        commandGateway.sendAndWait(command);
        return otpFuture.thenApply(result -> {
            System.out.println(result.getEmail());
            return  result;
        });
    }

    public void connectChatProfile(String idAccount) {
        Optional<Account> accountOptional = accountRepository.findById(idAccount);
        Account account = accountOptional.get();
        ConnectChatProfileCommand command = new ConnectChatProfileCommand(account.getIdChatProfile());
//        CompletableFuture<String> future = new CompletableFuture<>();
        commandGateway.send(command);
    }


    public void disConnect(String idAccount) {
       Account accountOptional = accountRepository.findByIdProfile(idAccount);
        DisConnectChatCommand command = new DisConnectChatCommand(accountOptional.getIdChatProfile());
//        CompletableFuture<String> future = new CompletableFuture<>();
        System.out.println(command.getIdChatProfile().toString());
        commandGateway.send(command);
    }

    public CompletableFuture<AccountCommandResponse> forgotPassword(ForgotPasswordReset forgotPasswordReset,  String email) {
        Account account = accountRepository.findByEmail(email);
        System.out.println(forgotPasswordReset);
        if(account==null){
            throw new AppException(AppErrorCode.ACCOUNT_NOT_EXISTED);
        }
        if(!forgotPasswordReset.getNewPassword().equals(forgotPasswordReset.getNewPassword2())){
            throw new AppException(AppErrorCode.PASSWORDS_INVALID);
        }
        AccountResetPasswordCommand command = new AccountResetPasswordCommand();
        command.setIdAccount(account.getIdAccount());
        command.setNewPassword(forgotPasswordReset.getNewPassword());
        command.setOtp(forgotPasswordReset.getOtp());
        command.setExecuteAt(new Date());

        CompletableFuture<String> future = new CompletableFuture<>();
        FutureTracker.futures.put(command.getIdAccount(), future);
        commandGateway.sendAndWait(command);
        return future.thenApply(result -> {
            AccountCommandResponse accountCommandResponse = new AccountCommandResponse();
            accountCommandResponse.setIdAccount(result);
            return accountCommandResponse;
        });
    }
}



