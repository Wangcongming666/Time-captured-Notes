package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/InboxMapper.xml.
 */
@Mapper
public interface InboxMapper extends BaseMapper<InboxEntity> {
  long inboxTransactionsConsume(@Param("consumer") String consumer, @Param("value1") String value1);

  int inboxTransactionsConsume2(@Param("consumer") String consumer, @Param("value1") String value1);
}
