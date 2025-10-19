package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.PaymentMethod;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.PaymentMethodRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentMethod addPaymentMethod(Long userId, String name, String mobileNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (paymentMethodRepository.findByUserUserIdAndName(userId, name).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PaymentMethodAlreadyExists", "Payment method with this name already exists for the user.");
        }

        if ("Mpesa".equalsIgnoreCase(name) && (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty())) {
            user.setPhoneNumber(mobileNumber);
            userRepository.save(user);
        }

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setUser(user);
        paymentMethod.setName(name);
        paymentMethod.setMobileNumber(mobileNumber);

        return paymentMethodRepository.save(paymentMethod);
    }

    public List<PaymentMethod> getPaymentMethodsByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        return paymentMethodRepository.findByUserUserId(userId);
    }
}
