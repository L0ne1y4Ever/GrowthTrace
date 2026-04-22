package com.growthtrace.diagnosis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GrowthSnapshotMapper extends BaseMapper<GrowthSnapshot> {
}
