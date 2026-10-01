package com.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthPrincipal implements UserDetails {

    private Long id;
    private String login;
    private String password; 
    private String role;    
    private String phoneNumber;
    private Boolean isEnabled;
    private Boolean isAccountNonLocked;

    public AuthPrincipal(AuthPrincipalDto dto){
        this.id = dto.getId();
        this.login = dto.getLogin();
        this.password = dto.getPassword(); 
        this.role = dto.getRole(); 
        this.phoneNumber = dto.getPhoneNumber();
        this.isEnabled = dto.getIsEnabled();
        this.isAccountNonLocked = dto.getIsAccountNonLocked();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return isAccountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return isEnabled; }
}
