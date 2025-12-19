package cn.edu.ndky.nkctf.mapper;

import cn.edu.ndky.nkctf.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章 Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

  /**
   * 增加文章浏览量
   */
  @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{articleId}")
  void incrementViewCount(@Param("articleId") Long articleId);
}
