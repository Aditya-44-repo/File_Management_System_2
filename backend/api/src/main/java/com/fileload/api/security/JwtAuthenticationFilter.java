package com.fileload.api.security;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.dao.repository.EmailChangeRequestRepository;
import com.fileload.model.entity.EmailChangeRequestStatus;
import com.fileload.model.entity.UserAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//JwtAuthenticationFilter is a Spring @Component that extends OncePerRequestFilter. Its job is to inspect incoming HTTP requests for a Bearer JWT, validate it,
// check the user's token version in the database, and if everything matches, populate the Spring Security context with an authenticated UsernamePasswordAuthenticationToken.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    //gets all the details related the user
    private final CustomUserDetailsService userDetailsService;
    //UserAccountRepository = data-access layer (talks to the database, returns UserAccount entities).
    //CustomUserDetailsService = security service (implements Spring Security's UserDetailsService, converts user data into a UserDetails object used by Spring Security).
    private final UserAccountRepository userAccountRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;

    //JwtUtil jwtUtil — token generation/validation + claim extraction.
    //(to check stored tokenVersion).
    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   UserAccountRepository userAccountRepository,
                                   EmailChangeRequestRepository emailChangeRequestRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userAccountRepository = userAccountRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
    }

    //Validate token and read claims
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String username = null;
        String token = null;
        Integer tokenVersion = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                username = jwtUtil.extractUsername(token);
                tokenVersion = jwtUtil.extractTokenVersion(token);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserAccount account = userAccountRepository.findByEmail(username).orElse(null);
            if (account == null) {
                account = emailChangeRequestRepository
                        .findFirstByOldEmailAndStatusOrderByReviewedAtDesc(username, EmailChangeRequestStatus.approved)
                        .map(emailChangeRequest -> emailChangeRequest.getUser())
                        .orElse(null);
            }
            if (account == null || tokenVersion == null || account.getTokenVersion() != tokenVersion) {
                filterChain.doFilter(request, response);
                return;
            }

            //Creates UsernamePasswordAuthenticationToken with user details and authorities and sets it into SecurityContextHolder.
            UserDetails userDetails = userDetailsService.loadUserByUsername(account.getEmail());
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        //Calls filterChain.doFilter(request, response); (whether authentication was set or not).
        filterChain.doFilter(request, response);
    }
}
