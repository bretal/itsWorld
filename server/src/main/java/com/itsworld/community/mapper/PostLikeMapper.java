package com.itsworld.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.community.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区帖子点赞数据访问接口
 *
 * @author Claude
 * @date 2026-11-05
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
