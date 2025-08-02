package com.lvhui.mianshiOj.esdao;

import com.lvhui.mianshiOj.model.dto.question.QuestionEsDTO;
import com.lvhui.mianshiOj.model.entity.Post;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目 ES 操作
 */
public interface QuestionEsDao 
    extends ElasticsearchRepository<QuestionEsDTO, Long> {
        List<PostEsDao> findByUserId(Long userId);
}
