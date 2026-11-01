package cn.shiguang.notes.modules.identity.infrastructure;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
/** Actual MyBatis-Plus CRUD integration, not an unused dependency. */
@Mapper
public interface UserMapper extends BaseMapper<UserRecord> {}
