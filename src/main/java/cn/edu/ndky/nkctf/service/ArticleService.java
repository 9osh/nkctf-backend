package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.ArticleQueryRequest;
import cn.edu.ndky.nkctf.dto.request.CreateArticleRequest;
import cn.edu.ndky.nkctf.dto.request.ReviewArticleRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateArticleRequest;
import cn.edu.ndky.nkctf.dto.response.ArticleDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ArticleListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;

/**
 * 文章服务接口
 */
public interface ArticleService {

  // ========== 公开接口 ==========

  /**
   * 获取已发布文章列表（公开访问）
   */
  PageResponse<ArticleListItemResponse> getPublishedArticles(ArticleQueryRequest request);

  /**
   * 获取已发布文章详情（公开访问，增加浏览量）
   */
  ArticleDetailResponse getPublishedArticleDetail(Long articleId);

  // ========== 用户接口 ==========

  /**
   * 创建文章草稿
   */
  ArticleDetailResponse createArticle(CreateArticleRequest request);

  /**
   * 更新自己的文章（仅 DRAFT 或 REJECTED 状态可编辑）
   */
  ArticleDetailResponse updateArticle(UpdateArticleRequest request);

  /**
   * 删除自己的草稿
   */
  void deleteArticle(Long articleId);

  /**
   * 提交文章审核
   */
  void submitForReview(Long articleId);

  /**
   * 获取当前用户的文章列表（包含所有状态）
   */
  PageResponse<ArticleListItemResponse> getMyArticles(ArticleQueryRequest request);

  /**
   * 获取当前用户的文章详情（包含所有状态）
   */
  ArticleDetailResponse getMyArticleDetail(Long articleId);

  // ========== 管理员接口 ==========

  /**
   * 管理员获取所有文章列表（支持状态过滤）
   */
  PageResponse<ArticleListItemResponse> getAllArticles(ArticleQueryRequest request);

  /**
   * 管理员获取任意文章详情
   */
  ArticleDetailResponse getArticleDetailForAdmin(Long articleId);

  /**
   * 审核文章（批准或拒绝）
   */
  void reviewArticle(ReviewArticleRequest request);

  /**
   * 管理员直接发布文章
   */
  ArticleDetailResponse createAndPublishArticle(CreateArticleRequest request);

  /**
   * 管理员删除任意文章
   */
  void deleteArticleByAdmin(Long articleId);
}
