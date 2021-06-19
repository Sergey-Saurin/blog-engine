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
            /**Разногласия в документации, написано что здесь нужно получить все посты с ModerationStatus.NEW
             * т.е. у модератора будет количество постов на модерацию вместе с черновиками, у которых isActive = 0
             * а в запросе /api/post/moderation в документации написано  что нужно посты с isActive = 1
             * поэтому количество будет разница, чтоб этого не было сделал здесь тоже только isActive = 1*/
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
        //Сгенерируем случайный код и запишем в базу
        String code = GeneralMethods.toGenerateRandomString(260);
        userModel.setCode(code);
        userRepository.save(userModel);

        //emailSender сервис отправки почтовых писем, все настройки почтового сервере в файле application.yml-mail:
        //сейчас настроен на mail.ru логин пароль тестовые
        //для проверки можно внести свои настройки
        emailSender.sendMessage(email, "http://localhost:8080/login/change-password/" + code);
        RestorePassword restorePassword = new RestorePassword();
        restorePassword.setResult(true);
        return restorePassword;
    }

    public RestorePassword changePassword(CodeRestorePasswordRequest resRequest) {

        //Проверить существует ли капча введенная с картинки и секретный код капчи
        Boolean isExist = captchaCodeRepository.checkIsExistCodeCaptcha(resRequest.getCaptcha(), resRequest.getCaptchaSecret());

        //Получить пользователя по коду для восстановления пароля
        User user = userRepository.findByCode(resRequest.getCode()).orElse(null);

        RestorePassword restorePassword = new RestorePassword();
        //Если нет кода капчи, пользователь не найден, пароль меньше 6 символовб вернуть ошибку
        if (isExist == null || user == null || resRequest.getPassword().length() < 6) {
            restorePassword.setErrors(restorePassword.new Error());
            return restorePassword;
        }

        //Закодировать пароль и сохранить в базу данных
        String encodePassword = passwordEncoder.encode(resRequest.getPassword());
        user.setPassword(encodePassword);
        userRepository.save(user);
        restorePassword.setResult(true);

        return restorePassword;
    }

    public RegistrationResponse registerUser(RegistrationRequest regRequest) {

        RegistrationResponse registrationResponse = new RegistrationResponse();

        User userIsExist = userRepository.findByEmail(regRequest.getEmail()).orElse(null);
        //Проверить существует ли капча введенная с картинки и секретный код капчи
        Boolean isExist = captchaCodeRepository.checkIsExistCodeCaptcha(regRequest.getCaptcha(), regRequest.getCaptchaSecret());

        //Вернуть ошибку если зарегистрован пользователь с таким емайлом, код капчи и секретный код капчи не найдены
        //пароль меньше 6 символов и имя не заполнено
        if (userIsExist != null || isExist == null ||
                regRequest.getPassword().length() < 6 || regRequest.getName().equals("")) {
            registrationResponse.setErrors(registrationResponse.new Error());
            return registrationResponse;
        }

        User user = new User();
        //new Date так как время в базе UTC, здесь будет текущее время, при сохранении сущности в базу данных
        //дата время будет в UTC
        user.setRegTime(new Date());
        user.setName(regRequest.getName());
        user.setEmail(regRequest.getEmail());
        //Закодировать пароль и сохранить в базу данных
        String encodePassword = passwordEncoder.encode(regRequest.getPassword());
        user.setPassword(encodePassword);

        userRepository.save(user);
        registrationResponse.setResult(true);

        return registrationResponse;
    }

    public Captcha createCaptcha() {

        GCage cage = new GCage();

        //Сгенерировать код и секретный код капчи
        String code = cage.getTokenGenerator().next();
        String secretCode = GeneralMethods.toGenerateRandomString(260);

        //Сохранить в базу данных
        CaptchaCode captchaCode = new CaptchaCode();
        captchaCode.setTime(new Date());
        captchaCode.setCode(code);
        captchaCode.setSecretCode(secretCode);
        captchaCodeRepository.save(captchaCode);
        //Созадть капчу
        byte[] imageInByte = createImageCaptcha(cage, code);

        //Конвертировать в Base64 капчу
        String encodedImage = Base64.getEncoder().encodeToString(imageInByte);
        //Создать DTO объект
        Captcha captcha = new Captcha();
        captcha.setSecret(secretCode);
        captcha.setImage("data:image/png;base64, " + encodedImage);

        //Удалить устаревшие капчи из таблицы
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, captchaLifetime * -1);

        captchaCodeRepository.deleteAllOlderDate(calendar.getTime());
        return captcha;
    }

    private byte[] createImageCaptcha(GCage cage, String code) {
        int imageWidth = 100;
        int imageHeight = 35;
        //Считать сформированную капчу в изображение(
        BufferedImage image = null;
        try {
            image = ImageIO.read(new ByteArrayInputStream(cage.draw(code)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //)
        //Установить размер изображения(
        BufferedImage scaledBI = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g.dispose();
        //)
        //Конвертировать изображение обрабтно в byte[](
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















