package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.ArticleTagRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章-标签关联 Mapper
 */
@Mapper
public interface ArticleTagRelationMapper extends BaseMapper<ArticleTagRelation> {

  /**
   * 根据标签ID查询文章ID列表
   */
  @Select("SELECT article_id FROM article_tag_relation WHERE tag_id = #{tagId}")
  List<Long> selectArticleIdsByTagId(@Param("tagId") Long tagId);
}
