package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import java.util.Collections;

public class WithMockAuthMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockAuthMember> {
    @Override
    public SecurityContext createSecurityContext(WithMockAuthMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AuthMember authMember = new AuthMember(annotation.memberId());
        Authentication auth = new UsernamePasswordAuthenticationToken(authMember, null, Collections.emptyList());
        context.setAuthentication(auth);
        return context;
    }
}
