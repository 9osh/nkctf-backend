package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.dto.Result;
import cn.edu.ndky.nkctf.dto.request.ArticleQueryRequest;
import cn.edu.ndky.nkctf.dto.request.CreateArticleRequest;
import cn.edu.ndky.nkctf.dto.request.CreateTagRequest;
import cn.edu.ndky.nkctf.dto.request.ReviewArticleRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateTagRequest;
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

/**
 * 管理员文章管理控制器
 */
@Tag(name = "管理员-学习指南", description = "管理员文章管理接口")
@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

  private final ArticleService articleService;
  private final ArticleTagService articleTagService;

  @Operation(summary = "获取所有文章列表", description = "管理员获取所有文章，支持状态和作者过滤")
  @GetMapping
  public Result<PageResponse<ArticleListItemResponse>> getAllArticles(
      @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
      @Parameter(description = "作者 ID") @RequestParam(required = false) Long authorId,
      @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page) {
    ArticleQueryRequest request = new ArticleQueryRequest();
    request.setStatus(status);
    request.setAuthorId(authorId);
    request.setKeyword(keyword);
    request.setPage(page);
    return Result.success(articleService.getAllArticles(request));
  }

  @Operation(summary = "获取文章详情", description = "管理员获取任意文章详情")
  @GetMapping("/{id}")
  public Result<ArticleDetailResponse> getArticleDetail(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    return Result.success(articleService.getArticleDetailForAdmin(id));
  }

  @Operation(summary = "审核文章", description = "批准或拒绝待审核的文章")
  @PostMapping("/review")
  public Result<Void> reviewArticle(@Valid @RequestBody ReviewArticleRequest request) {
    articleService.reviewArticle(request);
    String message = Boolean.TRUE.equals(request.getApproved()) ? "文章已发布" : "文章已拒绝";
    return Result.success(message, null);
  }

  @Operation(summary = "直接发布文章", description = "管理员创建并直接发布文章")
  @PostMapping("/publish")
  public Result<ArticleDetailResponse> createAndPublishArticle(
      @Valid @RequestBody CreateArticleRequest request) {
    return Result.success(articleService.createAndPublishArticle(request));
  }

  @Operation(summary = "删除文章", description = "管理员删除任意文章")
  @DeleteMapping("/{id}")
  public Result<Void> deleteArticle(
      @Parameter(description = "文章 ID") @PathVariable Long id) {
    articleService.deleteArticleByAdmin(id);
    return Result.success("删除成功", null);
  }

  // ========== 标签管理 ==========

  @Operation(summary = "创建标签", description = "创建新的文章标签")
  @PostMapping("/tags")
  public Result<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
    return Result.success(articleTagService.createTag(request));
  }

  @Operation(summary = "更新标签", description = "更新文章标签")
  @PutMapping("/tags")
  public Result<TagResponse> updateTag(@Valid @RequestBody UpdateTagRequest request) {
    return Result.success(articleTagService.updateTag(request));
  }

  @Operation(summary = "删除标签", description = "删除文章标签")
  @DeleteMapping("/tags/{id}")
  public Result<Void> deleteTag(
      @Parameter(description = "标签 ID") @PathVariable Long id) {
    articleTagService.deleteTag(id);
    return Result.success("删除成功", null);
  }
}
