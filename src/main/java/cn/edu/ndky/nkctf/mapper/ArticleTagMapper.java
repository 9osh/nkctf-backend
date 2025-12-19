package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.ArticleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章标签 Mapper
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

  /**
   * 统计标签被使用的文章数量（仅计算已发布的文章）
   */
  @Select("""
      SELECT COUNT(DISTINCT r.article_id)
      FROM article_tag_relation r
      JOIN article a ON r.article_id = a.id
      WHERE r.tag_id = #{tagId}
        AND a.status = 'PUBLISHED'
        AND a.deleted = 0
      """)
  Integer countPublishedArticlesByTag(@Param("tagId") Long tagId);
}
