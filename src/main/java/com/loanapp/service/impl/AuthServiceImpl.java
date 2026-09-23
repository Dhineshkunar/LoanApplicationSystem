package com.loanapp.service.impl;



import com.loanapp.dto.LoginRequest;
import com.loanapp.dto.LoginResponse;
import com.loanapp.entity.UserAccount;
import com.loanapp.repository.UserAccountRepository;
import com.loanapp.service.AuthService;
import com.loanapp.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LogManager.getLogger(AuthServiceImpl.class);
    public static final String ALPHABHETS_LOWER = "bdghjkmnpqrtvwxyz";
    public static final String ALPHABHETS_UPPER = ALPHABHETS_LOWER.toUpperCase()+"L";
    public static final String NUMBERS = "23456789";
    private static final int IMAGE_WIDTH = 160;
    private static final int IMAGE_HEIGHT = 40;
    private static final int TEXT_SIZE = 20;
//    private final SettingRepository settingRepository;

    private final UserAccountRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

//    private final UserLoginStatusRepository userLoginStatusRepository;
    private static final Integer EXPIRE_MINS = 10;
//    private final OTPCacheService otpCache;
//    private LoadingCache<String, String> otpCachee;
    @Override
    public LoginResponse login(LoginRequest request) {

        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            if (auth.isAuthenticated()) {
                return successfulLogin(request.getUsername());
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return unsuccessfulLogin();
    }
//
//    @Override
//    public StatusMessage generateCaptcha(LoginRequest request) {
//        StatusMessage msg = new StatusMessage();
//        try {
////            boolean captchaEnabled = "1".equals(getSettingValue("captchaenabled", "OTPValidation"));
//            List<UserAccount> uaccList = repository.findByUserName(request.getUsername());
//            UserAccount account = uaccList.isEmpty()?null:uaccList.get(0);
//            if (account == null) {
//                msg.setStatus("false");
//                msg.setCaptchaRequired(false);
//                msg.setMessage("Invalid Username.");
//                msg.setValue("");
//                msg.setUserMode("");
//            } else {
//
////                if (Boolean.TRUE.equals(officeStaff) && captchaEnabled) {
//                String captcha = insertSecCode("captcha", account, 6);
//                String captchaImage = generateCaptchaImage(captcha);
//                msg.setStatus("true");
//                msg.setCaptchaRequired(true);
//                msg.setMessage("success");
//                msg.setValue(captchaImage);
////                } else {
////                    msg.setStatus("false");
////                    msg.setCaptchaRequired(false);
////                    msg.setMessage("Captcha not required");
////                    msg.setValue("");
////                }
//            }
//        } catch (Exception e) {
//            logger.error("EXCEPTION", e);
//        }
//        return msg;
//    }

//    @Override
//    public LoginResponse generateOTP(LoginRequest login) {
//        List<UserAccount> uaccList = repository.findByUserName(login.getUsername());
//        UserAccount account = uaccList.isEmpty() ? null : uaccList.get(0);
//        String otp = otpCache.generateOTP(login.getUsername());
//        account.setOtpValue(otp);
//        repository.save(account);
//        return null;
//    }
//
//    @Override
//    public LoginResponse verifyOtp(LoginRequest login) {
//        LoginResponse lr = new LoginResponse();
//        List<UserAccount> uaccList = repository.findByUserName(login.getUsername());
//        UserAccount account = uaccList.isEmpty() ? null : uaccList.get(0);
//        if (login.getOtp().equals(account.getOtpValue())) {
//
//            lr.setMessage("valid");
//            return lr;
//        }else{
//            lr.setMessage("invalid");
//            return lr;
//        }}
//    @Override
//    public ResponseEntity<String> verifyNameForCaptcha(String username) {
//        AESCipher aes = new AESCipher();
//
//        try {
//            Optional<UserAccount> userOpt = repository.findFirstByUserName(username);
//            if (userOpt.isEmpty()) {
//                return ResponseEntity.ok(aes.encrypt(Map.of("message", "captcha is not required for this user")));
//            }
//
//            UserAccount user = userOpt.get();
//            if (user.getClient() != null && user.getClient().getClientId() != null && user.getClient().getClientId() == 104L) {
//                Optional<Setting> s = settingRepository.findFirstByNameAndEnvironmentType("CAPTCHA_ENABLED", "DCLocalServer");
//                String value = s.map(Setting::getValue).orElse("No");
//
//                if ("Yes".equalsIgnoreCase(value)) {
//                    return ResponseEntity.status(HttpStatus.CREATED)
//                            .body(aes.encrypt(Map.of("message", "captcha is required for this user")));
//                } else {
//                    return ResponseEntity.status(HttpStatus.CREATED)
//                            .body(aes.encrypt(Map.of("message", "captcha is not required for this user")));
//                }
//            }
//
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(aes.encrypt(Map.of("message", "captcha is not required for this user")));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Encryption error");
//        }
//    }

//    @Override
//    public Boolean checkUserSession(String username) {
//        JsonObject json = new JsonObject();
//        Optional<UserAccount> userOpt = repository.findFirstByUserName(username);
//
//        if (userOpt.isEmpty()) {
//            json.addProperty("tokenStatus", false);
//            return false;
//        }
//
//        UserAccount user = userOpt.get();
//        boolean isAlreadyToken = userLoginStatusRepository.existsByUserid_UserAccountId(user.getUserAccountId());
//
//        json.addProperty("tokenStatus", false);
//        return isAlreadyToken;
//    }


//    public static char[] generateOTP(int length) throws NoSuchAlgorithmException {
//        String numbers = NUMBERS;
//        Random random = SecureRandom.getInstanceStrong();
//        char[] otp = new char[length];
//        for (int i = 0; i < length; i++) {
//            otp[i] = numbers.charAt(random.nextInt(numbers.length()));
//        }
//        return otp;
//    }

//    public static String generateCaptchaImage(String captcha) {
//        String code = "";
//        try {
//            int x = 10;
//            int y = IMAGE_HEIGHT / 2 + TEXT_SIZE / 2;
//            BufferedImage bufferedImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
//            Graphics2D graphics = bufferedImage.createGraphics();
//            Font font = new Font("Arial", Font.CENTER_BASELINE, 30);
//            graphics.setFont(font);
//            graphics.setColor(new Color(68, 67, 67));
//            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//            char[] chars = captcha.toCharArray();
//            for (int i = 0; i < chars.length; i++) {
//                char ch = chars[i];
//                graphics.drawString(String.valueOf(ch), x + (font.getSize() - 5) * i,
//                        y + (int) Math.pow(-1, i) * (TEXT_SIZE / 6));
//            }
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            ImageIO.write(bufferedImage, "png", outputStream);
//            code = DatatypeConverter.printBase64Binary(outputStream.toByteArray());
//        } catch (Exception e) {
////            logger.error(EXCEPTION, e);
//        }
//        return code;
//    }
//    public String insertSecCode(String mode, UserAccount ua, Integer captchaSize) {
//        String captcha = generateAlphaNumericCaptcha(captchaSize);
//        ua.setCaptcha(captcha);
//        repository.save(ua);
//        return captcha;
//    }
//    private static String generateAlphaNumericCaptcha(int length) {
//        char[] otp = new char[length];
//        try {
//
//            Random random = SecureRandom.getInstanceStrong();
//
//            char num[] = generateOTP(length / 2, NUMBERS);
//            char alpha[] = generateOTP(length / 2, ALPHABHETS_LOWER + ALPHABHETS_UPPER);
//
//            boolean startWithNumber = random.nextBoolean();
//
//            if (startWithNumber) {
//                otp = mergeAlphaNumeric(length, num, alpha);
//            } else {
//                otp = mergeAlphaNumeric(length, alpha, num);
//            }
//        } catch (Exception e) {
//
//        }
//        return String.valueOf(otp);
//    }
//    private static char[] mergeAlphaNumeric(int length, char[] otp1, char[] otp2) {
//        char[] otp = new char[length];
//        otp[0] = otp1[0];
//        otp[1] = otp2[0];
//
//        otp[2] = otp1[1];
//        otp[3] = otp2[1];
//
//        otp[4] = otp1[2];
//        otp[5] = otp2[2];
//
//        if (length > 6) {
//            otp[6] = otp1[3];
//            otp[7] = otp1[3];
//        }
//        return otp;
//    }
//    public static char[] generateOTP(int length, String baseChars) throws NoSuchAlgorithmException {
//        String numbers = baseChars;
//        Random random = SecureRandom.getInstanceStrong();
//        char[] otp = new char[length];
//        for(int i = 0; i< length ; i++) {
//            otp[i] = numbers.charAt(random.nextInt(numbers.length()));
//        }
//        return otp;
//    }

//    @Override
    public LoginResponse successfulLogin(String username) {

        List<UserAccount> uaccList = repository.findByUserName(username);
        UserAccount account = uaccList.isEmpty()?null:uaccList.get(0);
        String token =  jwtService.generateToken(username,account.getId());

        LoginResponse lr = new LoginResponse();
        lr.setMessage("Login Successful");
        lr.setToken(token);
        lr.setUsername(account.getUsername());
        lr.setUserId(account.getId());
        return lr;
    }


//    @Override
    public LoginResponse unsuccessfulLogin() {
        LoginResponse lr = new LoginResponse();
        lr.setMessage("Invalid Username or Password");
        return lr;
    }

}