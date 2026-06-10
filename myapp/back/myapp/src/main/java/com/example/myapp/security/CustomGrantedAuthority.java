package com.example.myapp.security;

import org.springframework.security.core.GrantedAuthority;

public class CustomGrantedAuthority implements GrantedAuthority {
    private String roleName;
    public CustomGrantedAuthority(String roleName) {
        this.roleName = roleName;
    }
    @Override
    public String getAuthority() {
        return roleName;
    }
}
