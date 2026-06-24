package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockAuthMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockAuthMember> {
    @Override
    public SecurityContext createSecurityContext(WithMockAuthMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        MemberRole role = annotation.role();
        AuthMember authMember = new AuthMember(annotation.memberId(), role);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        context.setAuthentication(auth);
        return context;
    }
}
