package com.itangcent.model;

import java.util.List;

public class Node {

    /**
     * primary key
     */
    private String id;

    /**
     * org code
     */
    private String code;

    /**
     * sub orgs
     */
    private List<Node> sub;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Node> getSub() {
        return sub;
    }

    public void setSub(List<Node> sub) {
        this.sub = sub;
    }
}
