
package com.dpcoutreach.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dpcs")
public class Dpc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Account information can be expanded when authentication is implemented.
    private String accountInformation;

    protected Dpc() {
        // Required by JPA
    }

    public Dpc(String name, String accountInformation) {
        this.name = name;
        this.accountInformation = accountInformation;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountInformation() {
        return accountInformation;
    }

    public void setAccountInformation(String accountInformation) {
        this.accountInformation = accountInformation;
    }
}
