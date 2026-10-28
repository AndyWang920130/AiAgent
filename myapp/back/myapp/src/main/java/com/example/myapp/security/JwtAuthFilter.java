package com.example.myapp.security;

import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (jwtUtil.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtUtil.extractUsername(token);
            Optional<User> account = userRepository.findOneByLogin(username);
            // A token stays signed for a day, so the account is re-checked on every request:
            // disabling someone, or changing their role, takes effect at once instead of
            // whenever their token happens to expire. An unknown or disabled login is left
            // unauthenticated and the security rules answer 401/403 as usual.
            if (account.isPresent() && !Boolean.TRUE.equals(account.get().getDeleted())) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, currentAuthorities(account.get()));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    /** Authorities from the stored role, not from the token's claim, which may be stale. */
    private List<SimpleGrantedAuthority> currentAuthorities(User user) {
        String role = user.getRole() != null && !user.getRole().isBlank() ? user.getRole() : Role.USER.name();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
