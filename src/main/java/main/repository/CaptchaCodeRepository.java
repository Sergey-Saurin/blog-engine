package main.repository;

import main.model.CaptchaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
public interface CaptchaCodeRepository extends JpaRepository<CaptchaCode, Integer> {

    @Query("SELECT true " +
            "FROM CaptchaCode " +
            "WHERE code = :captcha AND secretCode = :captchaSecret")
    Boolean checkIsExistCodeCaptcha(String captcha, String captchaSecret);

    @Transactional
    @Modifying
    @Query("DELETE " +
            "FROM CaptchaCode c " +
            "WHERE c.time < :time")
    void deleteAllOlderDate(@Param("time") Date time);
}

