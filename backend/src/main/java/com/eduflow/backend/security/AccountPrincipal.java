package com.eduflow.backend.security;

import com.eduflow.backend.entity.AccountStatus;
import com.eduflow.backend.entity.User;

/**
 * Live database status and session version travel with the existing UserDetails lookup.
 */
public class AccountPrincipal extends org.springframework.security.core.userdetails.User {
    private final long securityVersion;

    public AccountPrincipal(User account) {
        super(account.getEmail(), account.getPassword(), account.getStatus() != AccountStatus.INACTIVE, true, true, account.getStatus() != AccountStatus.LOCKED, org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_" + account.getRole().name()));
        securityVersion = account.getSecurityVersion();
    }

    public long securityVersion() {
        return securityVersion;
    }
}
