package com.example.financeHub.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.financeHub.auth.model.User;

import lombok.Getter;

/**
 * Spring Security UserDetails 구현체.
 * roles(ROLE_ADMIN 등) + permissions(dashboard:read 등) 모두 GrantedAuthority로 등록.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;

        // 역할: ROLE_ADMIN, ROLE_USER
        List<GrantedAuthority> auths = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());

        // 권한: dashboard:read, admin:execute 등
        user.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(auths::add);

        this.authorities = auths;
    }

    @Override public String getPassword()                          { return user.getPassword(); }
    @Override public String getUsername()                          { return user.getUsername(); }
    @Override public boolean isAccountNonExpired()                 { return true; }
    @Override public boolean isAccountNonLocked()                  { return true; }
    @Override public boolean isCredentialsNonExpired()             { return true; }
    @Override public boolean isEnabled()                           { return user.isEnabled(); }

    public Long getId()                                            { return user.getId(); }
    public List<String> getRoleNames()                             {
        return user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList());
    }
    public List<String> getPermissionNames()                       { return user.getPermissions(); }
}
