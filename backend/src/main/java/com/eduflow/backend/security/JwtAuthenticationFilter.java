package com.eduflow.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityErrorHandler errors;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   SecurityErrorHandler errors) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                String email = jwtService.extractEmail(header.substring(7));
                UserDetails user = userDetailsService.loadUserByUsername(email);
                if (!user.isEnabled() || !user.isAccountNonLocked() || !(user instanceof AccountPrincipal account) || account.securityVersion() != jwtService.extractSecurityVersion(header.substring(7)))
                    throw new JwtException("Account session revoked");
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException exception) {
                SecurityContextHolder.clearContext();
                errors.write(response, 401, "JWT has expired");
                return;
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
                SecurityContextHolder.clearContext();
                errors.write(response, 401, "Invalid JWT");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
