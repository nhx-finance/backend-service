package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.ProductAccess;
import com.javaguy.nhxserver.repository.ProductAccessRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAccessService {

    private final ProductAccessRepository productAccessRepository;
    private final UserRepository userRepository;

    public List<ProductAccess> getProductAccessByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        return productAccessRepository.findByUserUserId(userId);
    }
}
