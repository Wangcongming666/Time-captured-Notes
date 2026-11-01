package cn.shiguang.notes.modules.identity.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Actual MyBatis-Plus CRUD integration, not an unused dependency. */
@Mapper
public interface UserMapper extends BaseMapper<UserRecord> {}
