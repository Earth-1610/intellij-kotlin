package com.itangcent.api;

import com.itangcent.api.GenericCtrl;
import com.itangcent.common.annotation.Public;
import com.itangcent.model.UserInfoDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * generic apis
 */
@RestController
@RequestMapping(value = "/user/detail")
public class UserDetailCtrl extends GenericCtrl<UserInfoDetail> {

    /**
     * say hello
     * not update anything
     */
    @Public
    @RequestMapping(value = "/greeting")
    public String greeting() {
        return "hello world";
    }

}
