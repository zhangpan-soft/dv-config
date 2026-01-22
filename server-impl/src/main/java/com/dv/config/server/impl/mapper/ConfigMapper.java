package com.dv.config.server.impl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dv.config.server.impl.entity.Config;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {
}
