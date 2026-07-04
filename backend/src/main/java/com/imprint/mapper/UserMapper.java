package com.imprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imprint.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
