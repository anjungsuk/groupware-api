package com.company.groupware.employee;

import com.company.groupware.common.entity.BaseSoftDeleteEntity;
import com.company.groupware.common.security.SystemRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 사원 — TRD §3.1 Employee.
 * 회원가입으로 생성될 때는 PENDING 이며, 부서·직급·입사일은 비어 있다.
 * 관리자가 승인하면서 소속을 배정하고 ACTIVE 로 전환한다.
 */
@Entity
@Table(name = "employees")
public class Employee extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, length = 20)
    private String employeeNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", nullable = false, length = 100)
    private String addressDetail;

    @Column(name = "mobile_phone", nullable = false, length = 20)
    private String mobilePhone;

    @Column(name = "home_phone", length = 20)
    private String homePhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_relation", nullable = false, length = 20)
    private EmergencyRelation emergencyRelation;

    @Column(name = "emergency_phone", nullable = false, length = 20)
    private String emergencyPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SystemRole role;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "position_code", length = 40)
    private String positionCode;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected Employee() {
    }

    private Employee(String employeeNo, String name, String email, String encodedPassword,
                     LocalDate birthDate, String zipCode, String address, String addressDetail,
                     String mobilePhone, String homePhone,
                     EmergencyRelation emergencyRelation, String emergencyPhone) {
        this.employeeNo = employeeNo;
        this.name = name;
        this.email = email;
        this.password = encodedPassword;
        this.birthDate = birthDate;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.mobilePhone = mobilePhone;
        this.homePhone = homePhone;
        this.emergencyRelation = emergencyRelation;
        this.emergencyPhone = emergencyPhone;
        this.status = EmployeeStatus.PENDING;
        this.role = SystemRole.MEMBER;
    }

    /** 회원가입으로 만들어지는 승인 대기 사원. 부서·직급은 승인 시 배정한다. */
    public static Employee pendingSignup(String employeeNo, String name, String email,
                                         String encodedPassword, LocalDate birthDate,
                                         String zipCode, String address, String addressDetail,
                                         String mobilePhone, String homePhone,
                                         EmergencyRelation emergencyRelation, String emergencyPhone) {
        return new Employee(employeeNo, name, email, encodedPassword, birthDate,
                zipCode, address, addressDetail, mobilePhone, homePhone,
                emergencyRelation, emergencyPhone);
    }

    /** 관리자 승인 — 소속을 배정하고 로그인 가능 상태로 전환한다. */
    public void approve(Long deptId, String positionCode, LocalDate hireDate) {
        this.deptId = deptId;
        this.positionCode = positionCode;
        this.hireDate = hireDate;
        this.status = EmployeeStatus.ACTIVE;
        this.approvedAt = Instant.now();
    }

    public void reject() {
        this.status = EmployeeStatus.REJECTED;
    }

    public boolean canLogin() {
        return status == EmployeeStatus.ACTIVE && !isDeleted();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public EmergencyRelation getEmergencyRelation() {
        return emergencyRelation;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public SystemRole getRole() {
        return role;
    }

    public Long getDeptId() {
        return deptId;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}
