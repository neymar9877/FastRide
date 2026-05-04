package com.example.bigproject;

public class User {
    private String id;
    private String UserName;
    private String password;
    private String phone;
    private String email;
    private String role; // 'passenger' || 'driver' || 'manager'
    private String image;

    public User() {}

    public User(String id, String UserName, String password, String email, String phone, String imageUrl, String role) {
        this.id = id;
        this.UserName = UserName;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.image = imageUrl;
    }

    //////////////////////////////////
    //           GETTERS            //
    //              &               //
    //           SETTERS            //
    //////////////////////////////////

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserName() { return UserName; }
    public void setUserName(String userName) { this.UserName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getImageUrl() { return image; }
    public void setImageUrl(String imageUrl) { this.image = imageUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
