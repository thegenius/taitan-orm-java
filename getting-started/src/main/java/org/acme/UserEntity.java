package org.acme;

import java.math.BigDecimal;
import java.util.Date;
import com.lvonce.taitan.Entity;
import com.lvonce.taitan.Id;
import com.lvonce.taitan.Column;
import lombok.Data;

@Entity
@Data
public class UserEntity {
    @Id
    private Long id;
    @Column private String name;
    @Column private Integer age; // 修改: 将 int 替换为 Integer
    @Column private BigDecimal salary;
    @Column private Date createdAt; // java.util.Date
}