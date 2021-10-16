package com.itangcent.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.itangcent.model.UserInfoDetail;

/**
 * login info
 */
public class UserLoginInfo extends UserInfoDetail {

    private Long loginTime;

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }
}
