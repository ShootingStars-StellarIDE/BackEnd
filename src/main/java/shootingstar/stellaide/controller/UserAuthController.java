package shootingstar.stellaide.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import shootingstar.stellaide.controller.dto.userAuth.ChangePasswordReqDto;
import shootingstar.stellaide.controller.dto.userAuth.CheckPasswordReqDto;
import shootingstar.stellaide.controller.dto.userAuth.LoginReqDto;
import shootingstar.stellaide.controller.dto.userAuth.SignupReqDto;
import shootingstar.stellaide.exception.CustomException;
import shootingstar.stellaide.security.jwt.JwtTokenProvider;
import shootingstar.stellaide.security.jwt.TokenInfo;
import shootingstar.stellaide.security.jwt.TokenProperty;
import shootingstar.stellaide.service.UserAuthService;

import static shootingstar.stellaide.exception.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenProperty tokenProperty;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupReqDto reqDto) {
        userAuthService.signup(reqDto.getEmail(), reqDto.getPassword(), reqDto.getNickname());
        return ResponseEntity.ok("성공적으로 회원가입 되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginReqDto reqDto, HttpServletRequest request, HttpServletResponse response) {
        String oldRefreshToken = getTokenFromCookie(request); // 쿠키에 존재 리프레시 토큰이 이미 있다면 가지고 온다.
        // 로그인 로직을 실행하여 JWT 토큰을 생성한다.
        TokenInfo tokenInfo = userAuthService.login(reqDto.getEmail(), reqDto.getPassword(), oldRefreshToken);
        // JWT 토큰에서 리프레시 토큰만 추출한다.
        String refreshToken = tokenInfo.getRefreshToken();

        // 쿠키에 리프레시 토큰 추가
        updateCookie(response, refreshToken, (tokenProperty.getREFRESH_EXPIRE() / 1000) - 1); // 쿠키 만료 시간, 리프레시 토큰의 만료 시간 보다 1분 적게 설정한다.

        HttpHeaders headers = new HttpHeaders(); // 헤더에 엑세스 토큰을 저장한다.
        headers.set("Authorization", "Bearer " + tokenInfo.getAccessToken());

        return ResponseEntity.ok().headers(headers).body("성공적으로 로그인 되었습니다.");
    }

    @DeleteMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getTokenFromCookie(request); // 쿠키에 존재하는 리프레시 토큰을 받아온다.
        String accessToken = getTokenFromHeader(request); // 헤더에 존재하는 엑세스 토큰을 받아온다.

        // accessToken 만료 제외 검증
        validateAccessToken(accessToken);
        // 리프레시 토큰 검증
        validateRefreshToken(refreshToken, response);

        // 로그아웃 로직 수행
        Authentication authentication = jwtTokenProvider.getAuthenticationFromRefreshToken(refreshToken);
        userAuthService.logout(refreshToken, authentication.getName());

        // 쿠키에 존재하는 리프레시 토큰을 제거한다.
        updateCookie(response, null, 0);

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    @DeleteMapping("/delete/user")
    public ResponseEntity<String> deleteUser(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getTokenFromCookie(request);   // 쿠키에 존재하는 리프레시 토큰을 받아온다.

        // 리프레시 토큰 검증
        validateRefreshToken(refreshToken, response);

        // 리프레시 토큰 무효화 검증
        if (!userAuthService.checkRefreshTokenState(refreshToken)) {
            throw new CustomException(EXPIRED_REFRESH_TOKEN);
        }
        Authentication authentication = jwtTokenProvider.getAuthenticationFromRefreshToken(refreshToken);
        // 회원 탈퇴 로직을 수행한다.
        userAuthService.deleteUser(refreshToken, authentication.getName());

        // 쿠키에 존재하는 리프레시 토큰을 제거한다.
        updateCookie(response, null, 0);

        return ResponseEntity.ok("성공적으로 회원탈퇴 되었습니다.");
    }

    @PostMapping("/checkPassword")
    public ResponseEntity<String> checkPassword(@RequestBody @Valid CheckPasswordReqDto reqDto, HttpServletRequest request) {
        String accessToken = getTokenFromHeader(request);

        if (!userAuthService.checkPassword(reqDto.getPassword(), accessToken)) {
            throw new CustomException(INCORRECT_VALUE_PASSWORD);
        }

        return ResponseEntity.ok().body("비밀번호 확인에 성공하였습니다.");
    }

    @PatchMapping("/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordReqDto reqDto, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = getTokenFromHeader(request);
        String refreshToken = getTokenFromCookie(request); // 쿠키에 존재하는 리프레시 토큰을 받아온다.

        // 리프레시 토큰 검증
        validateRefreshToken(refreshToken, response);

        // 리프레시 토큰 무효화 검증
        if (!userAuthService.checkRefreshTokenState(refreshToken)) {
            throw new CustomException(EXPIRED_REFRESH_TOKEN);
        }

        userAuthService.changePassword(reqDto.getPassword(), reqDto.getNewPassword(), accessToken, refreshToken);

        // 쿠키에 존재하는 리프레시 토큰을 제거한다.
        updateCookie(response, null, 0);

        return ResponseEntity.ok().body("비밀번호 변경에 성공하였습니다. 새로운 비밀번호로 로그인해 주세요.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getTokenFromCookie(request); // 쿠키에 존재하는 리프레시 토큰을 받아온다.
        String accessToken = getTokenFromHeader(request); // 헤더에 존재하는 엑세스 토큰을 받아온다.

        // accessToken 만료 제외 검증
        validateAccessToken(accessToken);
        // 리프레시 토큰 검증
        validateRefreshToken(refreshToken, response);

        // 리프레시 토큰 무효화 검증
        if (!userAuthService.checkRefreshTokenState(refreshToken)) {
            throw new CustomException(EXPIRED_REFRESH_TOKEN);
        }

        // 새로운 엑세스 토큰을 생성한다.
        String newAccessToken = userAuthService.refreshAccessToken(refreshToken);
        HttpHeaders headers = new HttpHeaders(); // 헤더에 액세스 토큰을 담아 반환한다.
        headers.set("Authorization", "Bearer " + newAccessToken);

        // 새 엑세스 토큰 반환
        return ResponseEntity.ok().headers(headers).body("Access Token 이 성공적으로 재발행 되었습니다.");
    }

    // 만료 검증을 제외한 access 토큰 검증 메서드
    private void validateAccessToken(String accessToken) {
        try {
            jwtTokenProvider.validateToken(accessToken);
        } catch (CustomException e) {
            if (!e.getErrorCode().equals(EXPIRED_ACCESS_TOKEN)) {
                throw new CustomException(AUTHENTICATION_ERROR);
            }
            log.info("위조되진 않았지만 만료된 Access Token 을 사용하셨습니다.");
        }
    }

    // 리프레시 토큰 검증 메서드
    private void validateRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            throw new CustomException(INVALID_REFRESH_TOKEN);
        }
        // 리프레시 토큰 검증
        try {
            jwtTokenProvider.validateRefreshToken(refreshToken);
        } catch (CustomException e) {
            updateCookie(response, null, 0);
            throw e;
        }
    }

    // 쿠키에서 리프레시 토큰을 추출하는 메서드
    private static String getTokenFromCookie(HttpServletRequest request) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        return refreshToken;
    }

    // 헤더에서 엑세스 토큰을 추출하는 메서드
    private static String getTokenFromHeader(HttpServletRequest request) {
        String token = request.getHeader("Authorization"); // 헤더에 존재하는 엑세스 토큰을 받아온다.

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 접두어 제거
        } else {
            throw new CustomException(INVALID_ACCESS_TOKEN);
        }
        return token;
    }

    // 쿠키의 리프레시 토큰 수정
    private static void updateCookie(HttpServletResponse response, String value, int age) {
        // 쿠키에 존재하는 리프레시 토큰을 삭제한다.
        Cookie cookie = new Cookie("refreshToken", value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
        cookie.setPath("/");
        cookie.setMaxAge(age);

        response.addCookie(cookie);
    }
}
