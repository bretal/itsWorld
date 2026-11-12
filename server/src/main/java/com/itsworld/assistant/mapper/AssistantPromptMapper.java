package com.itsworld.assistant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssistantPromptMapper {
    @Select("SELECT prompts FROM assistant_prompt_config WHERE id = 1")
    String findPrompts();

    @Insert("INSERT INTO assistant_prompt_config (id, prompts) VALUES (1, #{prompts}) "
            + "ON DUPLICATE KEY UPDATE prompts = #{prompts}")
    void savePrompts(@Param("prompts") String prompts);
}
