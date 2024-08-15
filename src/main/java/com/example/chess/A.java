package com.example.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class A {

    @JsonProperty("stu_name")
    private String stuName;
    @JsonProperty("stu_age")
    private Integer stuAge;
}
