package pe.com.sedapal.ofivirtual.domain.entity;

/**
 * Created by Hernan Pareja on 7/02/2017.
 */

public class User {

    private String password;
    private String currentPassword;
    private String newPassword;
    private String email;
    private String state;
    private String entryDate;
    private String userId;
    private String fullName;
    private String documentNumber;
    private String documentType;
    private String token;
    private String activationCode;
    private boolean saveEmail;
    private String phoneNumber;
    private String adverts;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public boolean isSaveEmail() {
        return saveEmail;
    }

    public void setSaveEmail(boolean saveEmail) {
        this.saveEmail = saveEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    public String getAdverts() {
        return adverts;
    }

    public void setAdverts(String adverts) {
        this.adverts = adverts;
    }

    @Override
    public String toString() {
        return fullName;
    }

}
