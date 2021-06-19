package main.controller;

import main.api.request.AuthRequest;
import main.api.request.CodeRestorePasswordRequest;
import main.api.request.EmailRestorePasswordRequest;
import main.api.request.RegistrationRequest;
import main.dto.AuthResponse;
import main.dto.Captcha;
import main.dto.RegistrationResponse;
import main.dto.RestorePassword;
import main.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    @Autowired
    private AuthService authService;

    /////////////////////////////////////////////////////////////

    /**
     * GET
     */
    @GetMapping("/check")
    public ResponseEntity<Object> getCheck(Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(new AuthResponse(), HttpStatus.OK);
        }
        return new ResponseEntity<>(authService.getAuthUserResponse(principal.getName()), HttpStatus.OK);

    }

    @GetMapping("/captcha")
    public ResponseEntity<Captcha> getCaptcha() {
        Captcha captcha = authService.createCaptcha();
        return new ResponseEntity<>(captcha, HttpStatus.OK);
    }

    @GetMapping("/logout")
    public ResponseEntity<AuthResponse> getLogout() {
        AuthResponse authResponse = authService.logoutUser();
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    /////////////////////////////////////////////////////////////

    /**
     * POST
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> postLogin(@RequestBody() AuthRequest authRequest) {
        AuthResponse authResponse = authService.authenticationUser(authRequest.getEmail(), authRequest.getPassword());
        return new ResponseEntity<>(authResponse, HttpStatus.OK);

    }

    @PostMapping("/restore")
    public ResponseEntity<RestorePassword> postRestore(@RequestBody EmailRestorePasswordRequest restorePasswordRequest) {
        RestorePassword restorePassword = authService.restorePassword(restorePasswordRequest);
        return new ResponseEntity<>(restorePassword, HttpStatus.OK);
    }

    @PostMapping("/password")
    public ResponseEntity<RestorePassword> postPassword(@RequestBody CodeRestorePasswordRequest codeRestorePasswordRequest) {
        RestorePassword restorePassword = authService.changePassword(codeRestorePasswordRequest);
        return new ResponseEntity<>(restorePassword, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> postRegister(@RequestBody RegistrationRequest registrationRequest) {
        RegistrationResponse registrationResponse = authService.registerUser(registrationRequest);
        return new ResponseEntity<>(registrationResponse, HttpStatus.OK);
    }


}











