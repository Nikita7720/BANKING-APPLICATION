package com.example.bank.entity;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity

@Table(name="AccountOpening")

public class AccountOpeningEntity {

 

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

private String fullname;
private String father;
private String gender;
private String dob;
private String email;
private String marital_status;
private String address;
private String city;
private String state;
private String pin;
private String religion;
private String category;
private String income;
private String qualification;
private String occupation;
private String pan;
private String aadhar;
private String senior_citizen;
private String existing_account ;
private String account_type ;
private String status="Pending";
private String rejectionReason;

private String accountNumber;
private String ifscCode;

private String customerId;

private String branchName;
private Double balance =0.0;
private String kycStatus;   // Pending / Verified
private String photoPath; 
@Transient
private MultipartFile file;
@Transient
private String otp; 
@ManyToOne
@JoinColumn(name = "user_id")
private User user;



@Lob
private byte[] photo;// image store path
        

        public String getFather() {
            return father;
        }

        public void setFather(String father) {
            this.father = father;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getDob() {
            return dob;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMaritalStatus() {
            return marital_status;
        }

        public void setMaritalStatus(String maritalStatus) {
            this.marital_status = maritalStatus;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPin() {
        return pin;
    }

        public void setPin(String pin) {
        this.pin = pin;
    }

        public String getReligion() {
            return religion;
        }

        public void setReligion(String religion) {
            this.religion = religion;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getIncome() {
        return income;
    }

    public void setIncome(String income) {
        this.income = income;
    }

        public String getQualification() {
            return qualification;
        }

        public void setQualification(String qualification) {
            this.qualification = qualification;
        }

        public String getOccupation() {
            return occupation;
        }

        public void setOccupation(String occupation) {
            this.occupation = occupation;
        }

        public String getPan() {
            return pan;
        }

        public void setPan(String pan) {
            this.pan = pan;
        }

        public String getAadhar() {
            return aadhar;
        }

        public void setAadhar(String aadhar) {
            this.aadhar = aadhar;
        }

        public String getSeniorCitizen() {
            return senior_citizen;
        }

        public void setSeniorCitizen(String seniorCitizen) {
            this.senior_citizen = seniorCitizen;
        }

        

        
        public String getMarital_status() {
            return marital_status;
        }

        public void setMarital_status(String marital_status) {
            this.marital_status = marital_status;
        }

        public String getSenior_citizen() {
            return senior_citizen;
        }

        public void setSenior_citizen(String senior_citizen) {
            this.senior_citizen = senior_citizen;
        }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExisting_account() {
        return existing_account;
    }

    public void setExisting_account(String existing_account) {
        this.existing_account = existing_account;
    }

    public String getAccount_type() {
        return account_type;
    }

    public void setAccount_type(String account_type) {
        this.account_type = account_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    

    

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    

  
    
    }
    



