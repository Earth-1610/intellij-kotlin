package com.itangcent.model;

import com.itangcent.constant.JavaVersion;
import com.itangcent.constant.MyNoArgConstant;
import com.itangcent.model.UserInfo;

import java.util.List;

class HugeModel {

    /**
     * single line
     *
     * @single
     * @desc low case of A
     * @prefix p-
     */
    private String a;

    /**
     * multi-line
     * second line
     *
     * @multi
     * @module x
     * x1 x2 x3
     * @module y
     * @suffix -s
     */
    private String b;

    /**
     * head line
     * second line
     * <pre>
     *     {
     *         "a":"b",
     *         "c":{
     *              "x":["y"]
     *         }
     *     }
     * </pre>
     * see {@link somelink}
     * tail line
     */
    private String c;

    /**
     * head line
     * second line
     * <pre>
     *
     *     {
     *         "a":"b",
     *         "c":{
     *              "x":["y"]
     *         }
     *     }
     *
     * </pre>
     * <p>
     * see {@link somelink}
     * tail line
     *
     * @type com.itangcent.model.Node
     */
    private String d;

    private String e;//E is a mathematical constant approximately equal to 2.71828

    /**
     * R, or r, is the eighteenth letter of the modern English alphabet and the ISO basic Latin alphabet.
     */
    private String r;//It's before s

    private String ignoreByGetter;

    private String ignoreBySetter;

    /**
     * candidates versions
     */
    private List<JavaVersion> candidates;

    /**
     * primary version
     */
    private JavaVersion version;

    /**
     * no arg constant
     */
    private MyNoArgConstant myNoArgConstant;

    /**
     * @unwrapped
     */
    private UserInfo userInfo;

    /**
     * @param a A
     * @param b B
     */
    private String methodA(int a, int b) {

    }

    /**
     * @ignore
     */
    public String getIgnoreByGetter() {
        return ignoreByGetter;
    }

    /**
     * @ignore
     */
    public void setIgnoreBySetter(String ignoreBySetter) {
        this.ignoreBySetter = ignoreBySetter;
    }
}