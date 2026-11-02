package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Actual MyBatis-Plus CRUD integration, not an unused dependency. */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {}
