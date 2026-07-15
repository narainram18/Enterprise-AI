package com.enterpriseai.backend.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.dto.UserProfileResponse;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.BadRequestException;
import com.enterpriseai.backend.mapper.UserMapper;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.specification.UserSpecifications;

@Service
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "email", "role", "createdAt", "updatedAt");

    private final UserRepository repository;
    private final UserMapper userMapper;

    public UserService(UserRepository repository, UserMapper userMapper) {
        this.repository = repository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> searchUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String search,
            String roleValue) {

        validatePage(page, size);
        String resolvedSortBy = resolveSortField(sortBy);
        Sort.Direction resolvedDirection = resolveDirection(direction);
        Role role = resolveRole(roleValue);

        Specification<User> specification = Specification.allOf(
                UserSpecifications.search(search),
                UserSpecifications.hasRole(role));

        Page<UserProfileResponse> users = repository
                .findAll(specification, PageRequest.of(
                        page,
                        size,
                        Sort.by(resolvedDirection, resolvedSortBy)))
                .map(userMapper::toProfileResponse);

        return PageResponse.from(users);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private String resolveSortField(String sortBy) {
        String value = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;
        if (!ALLOWED_SORT_FIELDS.contains(value)) {
            throw new BadRequestException("Unsupported sort field: " + value);
        }
        return value;
    }

    private Sort.Direction resolveDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() ->
                        new BadRequestException("Sort direction must be ASC or DESC"));
    }

    private Role resolveRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported role: " + roleValue);
        }
    }
}
