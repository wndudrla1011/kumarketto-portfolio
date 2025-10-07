package org.dsa11.team1.kumarketto.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.security.AuthenticatedUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    private static final String[] PUBLIC_URLS = {   //클래스 상수(static final)는 관례적으로 UPPER_SNAKE_CASE
            "/"
            ,"/css/**"
            ,"/js/**"
            ,"/images/**"
            ,"/api/**"
            ,"/product/detail/**"
            ,"/member/**"
            ,"/inquires" //문의글
            ,"/inquires/detail/**" //문의글 상세보기
            ,"/realtimechat/**" //실시간 채팅
    };

    @Lazy
    @Autowired
    private SavedRequestAwareAuthenticationSuccessHandler successHandler;

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler successHandler() {

//        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();

//        handler.setDefaultTargetUrl("/");
//        handler.setAlwaysUseDefaultTargetUrl(false);
//        handler.setTargetUrlParameter("redirect");

        return new SavedRequestAwareAuthenticationSuccessHandler() {

            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                HttpSession session = request.getSession();
                AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
                session.setAttribute("userId", authenticatedUser.getUsername());

                String redirectUrl = request.getParameter("redirect");
                if (redirectUrl == null || redirectUrl.isEmpty()) {
                    SavedRequest savedRequest = requestCache.getRequest(request, response);
                    if (savedRequest != null) {
                        redirectUrl = savedRequest.getRedirectUrl();
                        requestCache.removeRequest(request, response);
                    }
                }

                if (redirectUrl == null || redirectUrl.isEmpty()) redirectUrl = getDefaultTargetUrl();


                if("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=UTF-8");

                    log.debug("redirectUrl은 정상적으로 들어옵니까 이제 {}", redirectUrl);

                    Map<String, String> data = new HashMap<>();
                    data.put("redirect", redirectUrl);
                    response.getWriter().write(objectMapper.writeValueAsString(data));
                } else {
                    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                }

//                handler.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    public class AjaxAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void onAuthenticationFailure (HttpServletRequest request,
                                             HttpServletResponse response,
                                             AuthenticationException exception) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            Map<String, String> data = new HashMap<>();
            data.put("message", "IDまたはパスワードが正しくありません。");

            response.getWriter().write(objectMapper.writeValueAsString(data));
        }
    }

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 보호 자원 접근 시 자동으로 SaveRequest를 세션에 저장
                .requestCache(c -> c.requestCache(new HttpSessionRequestCache()))
                .authorizeHttpRequests(author -> author
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .anyRequest().authenticated()
                )
//                .httpBasic(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin
                        .loginPage("/member/signIn")
                        .usernameParameter("userId")
                        .passwordParameter("password")
                        .loginProcessingUrl("/member/signIn")
                        .successHandler(successHandler())
                        .failureHandler(new AjaxAuthenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/member/signOut")
                        .logoutSuccessUrl("/")
                );

        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder getPasswordEncoder() { return new BCryptPasswordEncoder(); }
}
