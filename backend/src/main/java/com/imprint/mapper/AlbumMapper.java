package com.imprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imprint.entity.Album;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
