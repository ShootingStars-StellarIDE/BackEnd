package shootingstar.stellaide.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MailServiceTest {

    @Autowired
    private MailService mailService;

    @Test
    public void sendEmail() throws Exception {
        String email = "skadu66@gmail.com";
        mailService.sendAuthCode(email);
    }
}