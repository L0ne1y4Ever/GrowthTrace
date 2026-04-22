package com.growthtrace.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.growthtrace.execution.entity.GrowthTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GrowthTaskMapper extends BaseMapper<GrowthTask> {
}
