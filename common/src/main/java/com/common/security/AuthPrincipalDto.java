package com.common.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthPrincipalDto {
    private Long id;
    private String login;
    private String password;
    private String role;
    private String phoneNumber;
    private Boolean isEnabled;
    private Boolean isAccountNonLocked;
}
