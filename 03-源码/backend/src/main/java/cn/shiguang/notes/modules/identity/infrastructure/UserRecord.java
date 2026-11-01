package cn.shiguang.notes.modules.identity.infrastructure;
import com.baomidou.mybatisplus.annotation.*;
/** MyBatis-Plus persistence object, intentionally not a domain/HTTP DTO. */
@TableName("t_user")
public class UserRecord {
    @TableId(type=IdType.AUTO) public Long id;
    public String username;
    public String passwordHash;
    public String nickname;
    public String bio;
    public Long avatarMediaId;
    public String status;
    public Integer authVersion;
}
