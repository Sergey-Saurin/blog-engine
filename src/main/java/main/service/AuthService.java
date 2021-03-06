package main.service;

import com.github.cage.GCage;
import main.GeneralMethods;
import main.api.request.CodeRestorePasswordRequest;
import main.api.request.EmailRestorePasswordRequest;
import main.api.request.RegistrationRequest;
import main.dto.AuthResponse;
import main.dto.Captcha;
import main.dto.RegistrationResponse;
import main.dto.RestorePassword;
import main.enums.ModerationStatus;
import main.model.CaptchaCode;
import main.model.User;
import main.repository.CaptchaCodeRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;


@Service
public class AuthService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${captcha.lifetime}")
    int captchaLifetime;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final EmailSenderService emailSender;
    private final CaptchaCodeRepository captchaCodeRepository;

    public AuthService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       EmailSenderService emailSender,
                       CaptchaCodeRepository captchaCodeRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.emailSender = emailSender;
        this.captchaCodeRepository = captchaCodeRepository;
    }

    public AuthResponse authenticationUser(String email, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            SecurityContextHolder.getContext().setAuthentication(auth);
            org.springframework.security.core.userdetails.User userDetails = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            AuthResponse authUser = getAuthUserResponse(userDetails.getUsername());
            return authUser;
        } catch (Exception e) {
            return new AuthResponse();
        }
    }

    public AuthResponse logoutUser() {
        SecurityContextHolder.getContext().setAuthentication(null);
        return new AuthResponse(true, null);
    }

    public AuthResponse getAuthUserResponse(String userName) {

        User userModel = userRepository.findByEmail(userName).orElseThrow(() -> new UsernameNotFoundException("user " + userName + " not found"));

        AuthResponse authUser = new AuthResponse();
        if (userModel != null) {
            authUser.setResult(true);
            //User{
            AuthResponse.User user = authUser.new User();
            user.setId(userModel.getId());
            user.setName(userModel.getName());
            user.setPhoto(userModel.getPhoto());
            user.setEmail(userModel.getEmail());
            boolean isModerator = userModel.getIsModerator();
            user.setModeration(isModerator);
            user.setSettings(isModerator);
            /**?????????????????????? ?? ????????????????????????, ???????????????? ?????? ?????????? ?????????? ???????????????? ?????? ?????????? ?? ModerationStatus.NEW
             * ??.??. ?? ???????????????????? ?????????? ???????????????????? ???????????? ???? ?????????????????? ???????????? ?? ??????????????????????, ?? ?????????????? isActive = 0
             * ?? ?? ?????????????? /api/post/moderation ?? ???????????????????????? ????????????????  ?????? ?????????? ?????????? ?? isActive = 1
             * ?????????????? ???????????????????? ?????????? ??????????????, ???????? ?????????? ???? ???????? ???????????? ?????????? ???????? ???????????? isActive = 1*/
            user.setModerationCount(isModerator ? userRepository.countPostsForModeration(ModerationStatus.NEW, Short.parseShort("1")) : 0);
            //}User
            authUser.setUser(user);
        }
        return authUser;
    }

    public RestorePassword restorePassword(EmailRestorePasswordRequest restorePasswordRequest) {
        String email = restorePasswordRequest.getEmail();
        User userModel = userRepository.findByEmail(email).orElse(null);

        if (userModel == null) {
            return new RestorePassword();
        }
        //?????????????????????? ?????????????????? ?????? ?? ?????????????? ?? ????????
        String code = GeneralMethods.toGenerateRandomString(260);
        userModel.setCode(code);
        userRepository.save(userModel);

        //emailSender ???????????? ???????????????? ???????????????? ??????????, ?????? ?????????????????? ?????????????????? ?????????????? ?? ?????????? application.yml-mail:
        //???????????? ???????????????? ???? mail.ru ?????????? ???????????? ????????????????
        //?????? ???????????????? ?????????? ???????????? ???????? ??????????????????
        emailSender.sendMessage(email, "http://localhost:8080/login/change-password/" + code);
        RestorePassword restorePassword = new RestorePassword();
        restorePassword.setResult(true);
        return restorePassword;
    }

    public RestorePassword changePassword(CodeRestorePasswordRequest resRequest) {

        //?????????????????? ???????????????????? ???? ?????????? ?????????????????? ?? ???????????????? ?? ?????????????????? ?????? ??????????
        Boolean isExist = captchaCodeRepository.checkIsExistCodeCaptcha(resRequest.getCaptcha(), resRequest.getCaptchaSecret());

        //???????????????? ???????????????????????? ???? ???????? ?????? ???????????????????????????? ????????????
        User user = userRepository.findByCode(resRequest.getCode()).orElse(null);

        RestorePassword restorePassword = new RestorePassword();
        //???????? ?????? ???????? ??????????, ???????????????????????? ???? ????????????, ???????????? ???????????? 6 ?????????????????? ?????????????? ????????????
        if (isExist == null || user == null || resRequest.getPassword().length() < 6) {
            restorePassword.setErrors(restorePassword.new Error());
            return restorePassword;
        }

        //???????????????????????? ???????????? ?? ?????????????????? ?? ???????? ????????????
        String encodePassword = passwordEncoder.encode(resRequest.getPassword());
        user.setPassword(encodePassword);
        userRepository.save(user);
        restorePassword.setResult(true);

        return restorePassword;
    }

    public RegistrationResponse registerUser(RegistrationRequest regRequest) {

        RegistrationResponse registrationResponse = new RegistrationResponse();

        User userIsExist = userRepository.findByEmail(regRequest.getEmail()).orElse(null);
        //?????????????????? ???????????????????? ???? ?????????? ?????????????????? ?? ???????????????? ?? ?????????????????? ?????? ??????????
        Boolean isExist = captchaCodeRepository.checkIsExistCodeCaptcha(regRequest.getCaptcha(), regRequest.getCaptchaSecret());

        //?????????????? ???????????? ???????? ?????????????????????????? ???????????????????????? ?? ?????????? ??????????????, ?????? ?????????? ?? ?????????????????? ?????? ?????????? ???? ??????????????
        //???????????? ???????????? 6 ???????????????? ?? ?????? ???? ??????????????????
        if (userIsExist != null || isExist == null ||
                regRequest.getPassword().length() < 6 || regRequest.getName().equals("")) {
            registrationResponse.setErrors(registrationResponse.new Error());
            return registrationResponse;
        }

        User user = new User();
        //new Date ?????? ?????? ?????????? ?? ???????? UTC, ?????????? ?????????? ?????????????? ??????????, ?????? ???????????????????? ???????????????? ?? ???????? ????????????
        //???????? ?????????? ?????????? ?? UTC
        user.setRegTime(new Date());
        user.setName(regRequest.getName());
        user.setEmail(regRequest.getEmail());
        //???????????????????????? ???????????? ?? ?????????????????? ?? ???????? ????????????
        String encodePassword = passwordEncoder.encode(regRequest.getPassword());
        user.setPassword(encodePassword);

        userRepository.save(user);
        registrationResponse.setResult(true);

        return registrationResponse;
    }

    public Captcha createCaptcha() {

        GCage cage = new GCage();

        //?????????????????????????? ?????? ?? ?????????????????? ?????? ??????????
        String code = cage.getTokenGenerator().next();
        String secretCode = GeneralMethods.toGenerateRandomString(260);

        //?????????????????? ?? ???????? ????????????
        CaptchaCode captchaCode = new CaptchaCode();
        captchaCode.setTime(new Date());
        captchaCode.setCode(code);
        captchaCode.setSecretCode(secretCode);
        captchaCodeRepository.save(captchaCode);
        //?????????????? ??????????
        byte[] imageInByte = createImageCaptcha(cage, code);

        //???????????????????????????? ?? Base64 ??????????
        String encodedImage = Base64.getEncoder().encodeToString(imageInByte);
        //?????????????? DTO ????????????
        Captcha captcha = new Captcha();
        captcha.setSecret(secretCode);
        captcha.setImage("data:image/png;base64, " + encodedImage);

        //?????????????? ???????????????????? ?????????? ???? ??????????????
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, captchaLifetime * -1);

        captchaCodeRepository.deleteAllOlderDate(calendar.getTime());
        return captcha;
    }

    private byte[] createImageCaptcha(GCage cage, String code) {
        int imageWidth = 100;
        int imageHeight = 35;
        //?????????????? ???????????????????????????? ?????????? ?? ??????????????????????(
        BufferedImage image = null;
        try {
            image = ImageIO.read(new ByteArrayInputStream(cage.draw(code)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //)
        //???????????????????? ???????????? ??????????????????????(
        BufferedImage scaledBI = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g.dispose();
        //)
        //???????????????????????????? ?????????????????????? ???????????????? ?? byte[](
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] imageInByte = new byte[0];
        try {
            ImageIO.write(scaledBI, "jpg", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //)
        return imageInByte;
    }
}















