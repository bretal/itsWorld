package com.itsworld.assistant.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.assistant.entity.AssistantQuota;
import org.apache.ibatis.annotations.*;
@Mapper
public interface AssistantQuotaMapper extends BaseMapper<AssistantQuota> {
    @Insert("INSERT INTO assistant_user_quota(user_id,hour_start,attempts) VALUES(#{userId},NOW(),0) ON DUPLICATE KEY UPDATE user_id=user_id")
    void ensure(@Param("userId") Long userId);
    @Select("SELECT * FROM assistant_user_quota WHERE user_id=#{userId} FOR UPDATE")
    AssistantQuota lock(@Param("userId") Long userId);
}
