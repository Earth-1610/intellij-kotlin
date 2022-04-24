package com.itangcent.constant;

public enum MyConstant {

    /**
     * 1st
     */
    ONE("a", 1.1f),

    /**
     * 2nd
     */
    TWO("b", 2.2f, "-s"),

    /**
     * 3rd
     */
    THREE(3.3f, "c"),

    /**
     * 4th
     */
    FOUR("d"),

    /**
     * 5th
     */
    FIVE(5.5f),

    /**
     * 6th
     */
    SIX("f", 6.6f, 7.7f, 8.8f),,

    /**
     * 7th
     */
    SEVEN
    ;

    /**
     * The standard name.
     */
    private final String name;

    /**
     * The float value.
     */
    private final float value;


    /**
     * Constructor
     */
    MyConstant() {
        this.value = 0;//default
        this.name = "default";
    }


    /**
     * Constructor.
     *
     * @param value the value
     * @param name  the name
     */
    MyConstant(final String name) {
        this.value = 0;//default
        this.name = name;
    }

    /**
     * Constructor.
     *
     * @param value the value
     * @param name  the name
     */
    MyConstant(final float value) {
        this.value = value;
        String x = String.valueOf(value);
        this.name = "default:" + x;
    }

    /**
     * Constructor.
     *
     * @param value the value
     * @param name  the name
     */
    MyConstant(final String name, final float value) {
        this.value = value;
        this.name = name;
    }

    /**
     * Constructor.
     *
     * @param value the value
     * @param name  the name
     */
    MyConstant(final String name, final float... values) {
        float t = 0f;
        for (float v : values) {
            t += v;
        }
        this.value = t;
        this.name = name;
    }

    /**
     * Constructor.
     *
     * @param value the value
     * @param name  the name
     */
    MyConstant(final String name, final float value, final String suffix) {
        this.value = value * 2;
        this.name = name + suffix;
    }

    /**
     * Constructor.
     *
     * @param v the value
     * @param n the name
     */
    MyConstant(final float v, final String n) {
        this.value = v;
        this.name = n;
    }
}
