package com.atguigu.gulimall.search.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@ToString
public class Account {

    /**
     * account_number : 970
     * balance : 19648
     * firstname : Forbes
     * lastname : Wallace
     * age : 28
     * gender : M
     * address : 990 Mill Road
     * employer : Pheast
     * email : forbeswallace@pheast.com
     * city : Lopezo
     * state : AK
     */

    private int accountNumber;
    private int balance;
    private String firstName;
    private String lastName;
    private int age;
    private String gender;
    private String address;
    private String employer;
    private String email;
    private String city;
    private String state;
}
