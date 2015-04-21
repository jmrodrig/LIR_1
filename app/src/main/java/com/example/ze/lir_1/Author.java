package com.example.ze.lir_1;

/**
 * Created by Ze on 11/04/2015.
 */
public class Author {
    private String fullName;
    private String email;
    private String avatarUrl;

    public Author() {}

    public String getFullName() {
        return fullName;
    }
    public String getAvatarUrl() {
        if (avatarUrl == null)
            return "http://lostinreality.net/assets/images/lir-logo.png";
        return avatarUrl;
    }
}
