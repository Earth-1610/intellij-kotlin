package com.itangcent.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.itangcent.model.UserInfo;

/**
 * user info detail
 */
public class UserInfoDetail extends UserInfo {

    private Integer level;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
