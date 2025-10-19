package com.javaguy.nhxserver.service.hedera;

import com.hedera.hashgraph.sdk.*;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.WalletRequestDto;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class HederaService {

    private final UserRepository userRepository;
    private final Client hederaClient;
    private final JavaMailSender mailSender;

    private User findUserAndCheckWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (user.getWalletAddress() != null) {
            throw new ApiException("Wallet already exists", HttpStatus.CONFLICT);
        }
        return user;
    }

    private void createHederaWallet(User user) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        PrivateKey privateKey = PrivateKey.generateED25519();
        PublicKey publicKey = privateKey.getPublicKey();
        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey)
                .setInitialBalance(Hbar.fromTinybars(1000));
        TransactionResponse txResponse = accountCreateTransaction.execute(hederaClient);
        AccountId accountId = txResponse.getReceipt(hederaClient).accountId;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your NSEVault Wallet");
        message.setText("Address: " + accountId + "\nPrivate Key: " + privateKey + "\nSave securely!");
        mailSender.send(message);

        if (accountId != null) {
            user.setWalletAddress(accountId.toString());
        }
    }

    private void linkExistingWallet(User user, String accountIdStr) {
        AccountId accountId = AccountId.fromString(accountIdStr);
        user.setWalletAddress(accountId.toString());
    }

//    //create, link, or manage a user's Hedera wallet
//    @Transactional
//    public void manageWallet(Long userId, @Valid WalletRequestDto dto) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
//        User user = findUserAndCheckWallet(userId);
//
//        if ("create".equalsIgnoreCase(dto.action())) {
//            createHederaWallet(user);
//        } else if ("link".equalsIgnoreCase(dto.action())) {
//            if (dto.accountId() == null) {
//                throw new ApiException("Account ID required for linking", HttpStatus.BAD_REQUEST);
//            }
//            linkExistingWallet(user, dto.accountId());
//        } else {
//            throw new ApiException("Invalid action", HttpStatus.BAD_REQUEST);
//        }
//        userRepository.save(user);
//    }
}
