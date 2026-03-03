package entities.guser;

import java.time.LocalDateTime;

public class User {
    private int id;

    private String firstname;
    private String lastname;
    private String email;
    private String passwordhash;
    private String roles; // CANDIDATE | RECRUITER | ADMIN
    private int isactive;

    private LocalDateTime lastloginat;
    private LocalDateTime createdat;

    private String type; // zeyda

    private String headline;
    private String bio;
    private String location;

    private String visibility;
    private String niveau;
    private Double scoreglobal;

    private String orgname;
    private String description;
    private String websiteurl;
    private String logourl;
    private String profilepic;

    private String school;
    private String degree;
    private String fieldofstudy;
    private Integer graduationyear;
    private String hardskills;
    private String softskills;
    private String githuburl;
    private String portfoliourl;
    private String phone;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordhash() { return passwordhash; }
    public void setPasswordhash(String passwordhash) { this.passwordhash = passwordhash; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public int getIsactive() { return isactive; }
    public void setIsactive(int isactive) { this.isactive = isactive; }

    public LocalDateTime getLastloginat() { return lastloginat; }
    public void setLastloginat(LocalDateTime lastloginat) { this.lastloginat = lastloginat; }

    public LocalDateTime getCreatedat() { return createdat; }
    public void setCreatedat(LocalDateTime createdat) { this.createdat = createdat; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public Double getScoreglobal() { return scoreglobal; }
    public void setScoreglobal(Double scoreglobal) { this.scoreglobal = scoreglobal; }

    public String getOrgname() { return orgname; }
    public void setOrgname(String orgname) { this.orgname = orgname; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsiteurl() { return websiteurl; }
    public void setWebsiteurl(String websiteurl) { this.websiteurl = websiteurl; }

    public String getLogourl() { return logourl; }
    public void setLogourl(String logourl) { this.logourl = logourl; }

    public String getProfilepic() { return profilepic; }
    public void setProfilepic(String profilepic) { this.profilepic = profilepic; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getFieldofstudy() { return fieldofstudy; }
    public void setFieldofstudy(String fieldofstudy) { this.fieldofstudy = fieldofstudy; }

    public Integer getGraduationyear() { return graduationyear; }
    public void setGraduationyear(Integer graduationyear) { this.graduationyear = graduationyear; }

    public String getHardskills() { return hardskills; }
    public void setHardskills(String hardskills) { this.hardskills = hardskills; }

    public String getSoftskills() { return softskills; }
    public void setSoftskills(String softskills) { this.softskills = softskills; }

    public String getGithuburl() { return githuburl; }
    public void setGithuburl(String githuburl) { this.githuburl = githuburl; }

    public String getPortfoliourl() { return portfoliourl; }
    public void setPortfoliourl(String portfoliourl) { this.portfoliourl = portfoliourl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
