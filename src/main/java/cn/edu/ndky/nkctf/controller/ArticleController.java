package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.ArticleQueryRequest;
import cn.edu.ndky.nkctf.dto.request.CreateArticleRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateArticleRequest;
import cn.edu.ndky.nkctf.dto.response.ArticleDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ArticleListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.dto.response.TagResponse;
import cn.edu.ndky.nkctf.service.ArticleService;
import cn.edu.ndky.nkctf.service.ArticleTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习指南文章控制器
 */
@Tag(name = "学习指南", description = "学习指南文章相关接口")
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleTagService articleTagService;

  // ========== 公开接口 ==========

  @Operation(summary = "获取已发布文章列表", description = "公开接口，获取所有已发布的文章")
  @GetMapping("/published")
  public Result<PageResponse<ArticleListItemResponse>> getPublishedArticles(
      @Parameter(description = "标签 ID") @RequestParam(required = false) Long tagId,
      @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
      @Parameter(description = "排序方式 (newest, most_views)")
      @RequestParam(required = false, defaultValue = "newest") String sortBy,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    ArticleQueryRequest request = new ArticleQueryRequest();
    request.setTagId(tagId);
    request.setKeyword(keyword);
    request.setSortBy(sortBy);
    request.setPage(page);
    return Result.success(articleService.getPublishedArticles(request));
  }

  @Operation(summary = "获取已发布文章详情", description = "公开接口，获取已发布文章的详细内容")
  @GetMapping("/published/{id}")
  public Result<ArticleDetailResponse> getPublishedArticleDetail(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    return Result.success(articleService.getPublishedArticleDetail(id));
  }

  @Operation(summary = "获取所有标签", description = "公开接口，获取所有文章标签")
  @GetMapping("/tags")
  public Result<List<TagResponse>> getAllTags() {
    return Result.success(articleTagService.getAllTags());
  }

  // ========== 用户接口（需登录） ==========

  @Operation(summary = "创建文章草稿", description = "创建新的文章草稿")
  @PostMapping
  public Result<ArticleDetailResponse> createArticle(
      @Valid @RequestBody CreateArticleRequest request) {
    return Result.success(articleService.createArticle(request));
  }

  @Operation(summary = "更新文章", description = "更新自己的文章（仅草稿或被拒绝状态可编辑）")
  @PutMapping
  public Result<ArticleDetailResponse> updateArticle(
      @Valid @RequestBody UpdateArticleRequest request) {
    return Result.success(articleService.updateArticle(request));
  }

  @Operation(summary = "删除文章", description = "删除自己的草稿")
  @DeleteMapping("/{id}")
  public Result<Void> deleteArticle(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    articleService.deleteArticle(id);
    return Result.success("删除成功", null);
  }

  @Operation(summary = "提交审核", description = "将草稿提交审核")
  @PostMapping("/{id}/submit")
  public Result<Void> submitForReview(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    articleService.submitForReview(id);
    return Result.success("提交成功，请等待审核", null);
  }

  @Operation(summary = "获取我的文章列表", description = "获取当前用户的所有文章")
  @GetMapping("/my")
  public Result<PageResponse<ArticleListItemResponse>> getMyArticles(
      @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    ArticleQueryRequest request = new ArticleQueryRequest();
    request.setStatus(status);
    request.setPage(page);
    return Result.success(articleService.getMyArticles(request));
  }

  @Operation(summary = "获取我的文章详情", description = "获取当前用户的文章详情（包含所有状态）")
  @GetMapping("/my/{id}")
  public Result<ArticleDetailResponse> getMyArticleDetail(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    return Result.success(articleService.getMyArticleDetail(id));
  }
}
