package com.itsworld.assistant.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AssistantInstructionMapper {
    record Snapshot(String draft, String published, String previous, long revision) {}
    @Select("SELECT draft, published, previous, revision FROM assistant_instruction_config WHERE id=1")
    Snapshot find();
    @Insert("INSERT INTO assistant_instruction_config(id,draft) VALUES(1,#{draft}) ON DUPLICATE KEY UPDATE draft=#{draft}")
    void saveDraft(@Param("draft") String draft);
    @Update("UPDATE assistant_instruction_config SET previous=COALESCE(published,#{defaults}), published=draft, revision=revision+1 WHERE id=1")
    int publish(@Param("defaults") String defaults);
    @Update("UPDATE assistant_instruction_config SET published=previous, previous=NULL, revision=revision+1 WHERE id=1 AND previous IS NOT NULL")
    int rollback();
}
