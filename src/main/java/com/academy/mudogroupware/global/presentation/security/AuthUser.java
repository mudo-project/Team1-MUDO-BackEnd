package com.academy.mudogroupware.global.presentation.security;
import java.security.Principal;
public record AuthUser(Long userId, String username, String role) implements Principal { @Override public String getName(){return username;} }
