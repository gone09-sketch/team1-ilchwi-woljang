package com.team1ilchwiwoljang.domain.order.controller;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAuthMemberSecurityContextFactory.class)
public @interface WithMockAuthMember {
    long memberId() default 1L;
}
