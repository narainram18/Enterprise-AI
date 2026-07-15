package com.enterpriseai.backend.specification;

import org.springframework.data.jpa.domain.Specification;

import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";

        // Search is deliberately limited to public user fields; credentials never enter the query.
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern));
    }

    public static Specification<User> hasRole(Role role) {
        if (role == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("role"), role);
    }
}
