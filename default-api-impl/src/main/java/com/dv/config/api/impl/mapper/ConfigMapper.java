package com.dv.config.api.impl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dv.config.api.impl.entity.Config;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {
}
