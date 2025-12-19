package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.CreateTagRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateTagRequest;
import cn.edu.ndky.nkctf.dto.response.TagResponse;

import java.util.List;

/**
 * 文章标签服务接口
 */
public interface ArticleTagService {

  /**
   * 获取所有标签列表（公开访问）
   */
  List<TagResponse> getAllTags();

  /**
   * 创建标签（管理员）
   */
  TagResponse createTag(CreateTagRequest request);

  /**
   * 更新标签（管理员）
   */
  TagResponse updateTag(UpdateTagRequest request);

  /**
   * 删除标签（管理员）
   */
  void deleteTag(Long tagId);
}
