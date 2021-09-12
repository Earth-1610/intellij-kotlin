package com.itangcent.api;

import com.itangcent.common.annotation.Public;
import com.itangcent.model.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * generic apis
 */
@RestController
@RequestMapping(value = "generic")
public class GenericCtrl<T> {

    /**
     * result
     */
    @Public
    @RequestMapping(value = "/result")
    public Result<T> result(T t) {
        return Result.success(t);
    }
}
