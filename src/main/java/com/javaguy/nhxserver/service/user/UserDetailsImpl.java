package com.javaguy.nhxserver.service.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.javaguy.nhxserver.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
@Getter
@Setter
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;


    private final Long id;
    private final String username;
    private final String email;
    private final String phoneNumber;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final boolean emailVerified;

    public UserDetailsImpl(Long id, String username, String email, String phoneNumber, String password,
                         Collection<? extends GrantedAuthority> authorities, boolean accountNonExpired,
                         boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled, boolean emailVerified) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name())).collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPassword(),
                authorities,
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                user.isEnabled(),
                user.isEmailVerified());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
