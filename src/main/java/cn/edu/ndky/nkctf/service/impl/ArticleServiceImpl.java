package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.ArticleQueryRequest;
import cn.edu.ndky.nkctf.dto.request.CreateArticleRequest;
import cn.edu.ndky.nkctf.dto.request.ReviewArticleRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateArticleRequest;
import cn.edu.ndky.nkctf.dto.response.ArticleDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ArticleListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.entity.Article;
import cn.edu.ndky.nkctf.entity.ArticleTag;
import cn.edu.ndky.nkctf.entity.ArticleTagRelation;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.enums.ArticleStatus;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ArticleMapper;
import cn.edu.ndky.nkctf.mapper.ArticleTagMapper;
import cn.edu.ndky.nkctf.mapper.ArticleTagRelationMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.ArticleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

  private final ArticleMapper articleMapper;
  private final ArticleTagMapper articleTagMapper;
  private final ArticleTagRelationMapper articleTagRelationMapper;
  private final UserMapper userMapper;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // ========== 公开接口 ==========

  @Override
  public PageResponse<ArticleListItemResponse> getPublishedArticles(ArticleQueryRequest request) {
    LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
        .eq(Article::getStatus, ArticleStatus.PUBLISHED.name());

    // 标签过滤
    if (request.getTagId() != null) {
      List<Long> articleIds = articleTagRelationMapper.selectArticleIdsByTagId(request.getTagId());
      if (articleIds.isEmpty()) {
        return emptyPageResponse(request);
      }
      wrapper.in(Article::getId, articleIds);
    }

    // 关键词搜索
    if (StringUtils.hasText(request.getKeyword())) {
      wrapper.like(Article::getTitle, request.getKeyword());
    }

    // 排序
    if ("most_views".equals(request.getSortBy())) {
      wrapper.orderByDesc(Article::getViewCount);
    } else {
      wrapper.orderByDesc(Article::getPublishTime);
    }

    return queryArticles(wrapper, request);
  }

  @Override
  public ArticleDetailResponse getPublishedArticleDetail(Long articleId) {
    Article article = articleMapper.selectById(articleId);
    if (article == null || !ArticleStatus.PUBLISHED.name().equals(article.getStatus())) {
      throw new BusinessException(404, "文章不存在");
    }

    // 增加浏览量
    articleMapper.incrementViewCount(articleId);
    article.setViewCount(article.getViewCount() + 1);

    return toArticleDetailResponse(article);
  }

  // ========== 用户接口 ==========

  @Override
  @Transactional
  public ArticleDetailResponse createArticle(CreateArticleRequest request) {
    User currentUser = getCurrentUser();

    Article article = new Article();
    article.setTitle(request.getTitle());
    article.setSummary(request.getSummary());
    article.setContent(request.getContent());
    article.setAuthorId(currentUser.getId());
    article.setStatus(ArticleStatus.DRAFT.name());
    article.setViewCount(0);

    articleMapper.insert(article);

    // 保存标签关联
    saveTagRelations(article.getId(), request.getTagIds());

    log.info("用户 {} 创建了文章草稿: {}", currentUser.getUsername(), article.getTitle());

    return toArticleDetailResponse(article);
  }

  @Override
  @Transactional
  public ArticleDetailResponse updateArticle(UpdateArticleRequest request) {
    User currentUser = getCurrentUser();

    Article article = articleMapper.selectById(request.getId());
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    // 验证所有权
    if (!article.getAuthorId().equals(currentUser.getId())) {
      throw new BusinessException(403, "无权编辑此文章");
    }

    // 验证状态（仅 DRAFT 或 REJECTED 可编辑）
    String status = article.getStatus();
    if (!ArticleStatus.DRAFT.name().equals(status) &&
        !ArticleStatus.REJECTED.name().equals(status)) {
      throw new BusinessException(400, "当前状态不允许编辑");
    }

    article.setTitle(request.getTitle());
    article.setSummary(request.getSummary());
    article.setContent(request.getContent());

    // 如果是 REJECTED 状态，修改后回到 DRAFT
    if (ArticleStatus.REJECTED.name().equals(status)) {
      article.setStatus(ArticleStatus.DRAFT.name());
      article.setReviewComment(null);
      article.setReviewerId(null);
      article.setReviewTime(null);
    }

    articleMapper.updateById(article);

    // 更新标签关联
    updateTagRelations(article.getId(), request.getTagIds());

    log.info("用户 {} 更新了文章: {}", currentUser.getUsername(), article.getTitle());

    return toArticleDetailResponse(article);
  }

  @Override
  @Transactional
  public void deleteArticle(Long articleId) {
    User currentUser = getCurrentUser();

    Article article = articleMapper.selectById(articleId);
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    // 验证所有权
    if (!article.getAuthorId().equals(currentUser.getId())) {
      throw new BusinessException(403, "无权删除此文章");
    }

    // 仅 DRAFT 状态可删除
    if (!ArticleStatus.DRAFT.name().equals(article.getStatus())) {
      throw new BusinessException(400, "只能删除草稿状态的文章");
    }

    articleMapper.deleteById(articleId);

    log.info("用户 {} 删除了文章草稿: {}", currentUser.getUsername(), article.getTitle());
  }

  @Override
  @Transactional
  public void submitForReview(Long articleId) {
    User currentUser = getCurrentUser();

    Article article = articleMapper.selectById(articleId);
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    // 验证所有权
    if (!article.getAuthorId().equals(currentUser.getId())) {
      throw new BusinessException(403, "无权操作此文章");
    }

    // 仅 DRAFT 状态可提交审核
    if (!ArticleStatus.DRAFT.name().equals(article.getStatus())) {
      throw new BusinessException(400, "只能提交草稿状态的文章");
    }

    article.setStatus(ArticleStatus.PENDING.name());
    articleMapper.updateById(article);

    log.info("用户 {} 提交了文章审核: {}", currentUser.getUsername(), article.getTitle());
  }

  @Override
  public PageResponse<ArticleListItemResponse> getMyArticles(ArticleQueryRequest request) {
    User currentUser = getCurrentUser();

    LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
        .eq(Article::getAuthorId, currentUser.getId());

    // 状态过滤
    if (StringUtils.hasText(request.getStatus())) {
      wrapper.eq(Article::getStatus, request.getStatus());
    }

    wrapper.orderByDesc(Article::getCreateTime);

    return queryArticles(wrapper, request);
  }

  @Override
  public ArticleDetailResponse getMyArticleDetail(Long articleId) {
    User currentUser = getCurrentUser();

    Article article = articleMapper.selectById(articleId);
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    // 验证所有权
    if (!article.getAuthorId().equals(currentUser.getId())) {
      throw new BusinessException(403, "无权查看此文章");
    }

    return toArticleDetailResponse(article);
  }

  // ========== 管理员接口 ==========

  @Override
  public PageResponse<ArticleListItemResponse> getAllArticles(ArticleQueryRequest request) {
    verifyAdminRole();

    LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

    // 状态过滤
    if (StringUtils.hasText(request.getStatus())) {
      wrapper.eq(Article::getStatus, request.getStatus());
    }

    // 作者过滤
    if (request.getAuthorId() != null) {
      wrapper.eq(Article::getAuthorId, request.getAuthorId());
    }

    // 关键词搜索
    if (StringUtils.hasText(request.getKeyword())) {
      wrapper.like(Article::getTitle, request.getKeyword());
    }

    wrapper.orderByDesc(Article::getCreateTime);

    return queryArticles(wrapper, request);
  }

  @Override
  public ArticleDetailResponse getArticleDetailForAdmin(Long articleId) {
    verifyAdminRole();

    Article article = articleMapper.selectById(articleId);
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    return toArticleDetailResponse(article);
  }

  @Override
  @Transactional
  public void reviewArticle(ReviewArticleRequest request) {
    User admin = getCurrentUser();
    verifyAdminRole();

    Article article = articleMapper.selectById(request.getArticleId());
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    // 仅 PENDING 状态可审核
    if (!ArticleStatus.PENDING.name().equals(article.getStatus())) {
      throw new BusinessException(400, "只能审核待审核状态的文章");
    }

    article.setReviewerId(admin.getId());
    article.setReviewTime(LocalDateTime.now());

    if (Boolean.TRUE.equals(request.getApproved())) {
      article.setStatus(ArticleStatus.PUBLISHED.name());
      article.setPublishTime(LocalDateTime.now());
      log.info("管理员 {} 批准了文章: {}", admin.getUsername(), article.getTitle());
    } else {
      if (!StringUtils.hasText(request.getComment())) {
        throw new BusinessException(400, "拒绝时必须填写理由");
      }
      article.setStatus(ArticleStatus.REJECTED.name());
      article.setReviewComment(request.getComment());
      log.info("管理员 {} 拒绝了文章: {}, 理由: {}",
          admin.getUsername(), article.getTitle(), request.getComment());
    }

    articleMapper.updateById(article);
  }

  @Override
  @Transactional
  public ArticleDetailResponse createAndPublishArticle(CreateArticleRequest request) {
    User admin = getCurrentUser();
    verifyAdminRole();

    Article article = new Article();
    article.setTitle(request.getTitle());
    article.setSummary(request.getSummary());
    article.setContent(request.getContent());
    article.setAuthorId(admin.getId());
    article.setStatus(ArticleStatus.PUBLISHED.name());
    article.setPublishTime(LocalDateTime.now());
    article.setViewCount(0);

    articleMapper.insert(article);

    // 保存标签关联
    saveTagRelations(article.getId(), request.getTagIds());

    log.info("管理员 {} 直接发布了文章: {}", admin.getUsername(), article.getTitle());

    return toArticleDetailResponse(article);
  }

  @Override
  @Transactional
  public void deleteArticleByAdmin(Long articleId) {
    User admin = getCurrentUser();
    verifyAdminRole();

    Article article = articleMapper.selectById(articleId);
    if (article == null) {
      throw new BusinessException(404, "文章不存在");
    }

    articleMapper.deleteById(articleId);

    log.info("管理员 {} 删除了文章: {}", admin.getUsername(), article.getTitle());
  }

  // ========== 私有方法 ==========

  private PageResponse<ArticleListItemResponse> queryArticles(
      LambdaQueryWrapper<Article> wrapper, ArticleQueryRequest request) {
    Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<Article> result = articleMapper.selectPage(page, wrapper);

    List<ArticleListItemResponse> records = result.getRecords().stream()
        .map(this::toArticleListItemResponse)
        .collect(Collectors.toList());

    int totalPages = (int) Math.ceil((double) result.getTotal() / request.getPageSize());

    return PageResponse.<ArticleListItemResponse>builder()
        .records(records)
        .total(result.getTotal())
        .page(request.getPageNum())
        .size(request.getPageSize())
        .pages(totalPages)
        .hasNext(request.getPageNum() < totalPages)
        .hasPrevious(request.getPageNum() > 1)
        .build();
  }

  private PageResponse<ArticleListItemResponse> emptyPageResponse(ArticleQueryRequest request) {
    return PageResponse.<ArticleListItemResponse>builder()
        .records(Collections.emptyList())
        .total(0L)
        .page(request.getPageNum())
        .size(request.getPageSize())
        .pages(0)
        .hasNext(false)
        .hasPrevious(false)
        .build();
  }

  private void saveTagRelations(Long articleId, List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }

    for (Long tagId : tagIds) {
      ArticleTag tag = articleTagMapper.selectById(tagId);
      if (tag != null) {
        ArticleTagRelation relation = new ArticleTagRelation();
        relation.setArticleId(articleId);
        relation.setTagId(tagId);
        articleTagRelationMapper.insert(relation);
      }
    }
  }

  private void updateTagRelations(Long articleId, List<Long> tagIds) {
    // 删除旧关联
    articleTagRelationMapper.delete(
        new LambdaQueryWrapper<ArticleTagRelation>()
            .eq(ArticleTagRelation::getArticleId, articleId)
    );

    // 保存新关联
    saveTagRelations(articleId, tagIds);
  }

  private List<ArticleDetailResponse.TagInfo> getArticleTags(Long articleId) {
    List<ArticleTagRelation> relations = articleTagRelationMapper.selectList(
        new LambdaQueryWrapper<ArticleTagRelation>()
            .eq(ArticleTagRelation::getArticleId, articleId)
    );

    return relations.stream()
        .map(r -> {
          ArticleTag tag = articleTagMapper.selectById(r.getTagId());
          if (tag == null) {
            return null;
          }
          return ArticleDetailResponse.TagInfo.builder()
              .id(tag.getId())
              .name(tag.getName())
              .color(tag.getColor())
              .build();
        })
        .filter(t -> t != null)
        .collect(Collectors.toList());
  }

  private ArticleDetailResponse toArticleDetailResponse(Article article) {
    User author = userMapper.selectById(article.getAuthorId());

    ArticleDetailResponse.AuthorInfo authorInfo = null;
    if (author != null) {
      authorInfo = ArticleDetailResponse.AuthorInfo.builder()
          .id(author.getId())
          .username(author.getUsername())
          .nickname(author.getNickname())
          .avatar(author.getAvatar())
          .build();
    }

    ArticleDetailResponse.ReviewerInfo reviewerInfo = null;
    if (article.getReviewerId() != null) {
      User reviewer = userMapper.selectById(article.getReviewerId());
      if (reviewer != null) {
        reviewerInfo = ArticleDetailResponse.ReviewerInfo.builder()
            .id(reviewer.getId())
            .username(reviewer.getUsername())
            .nickname(reviewer.getNickname())
            .build();
      }
    }

    return ArticleDetailResponse.builder()
        .id(article.getId())
        .title(article.getTitle())
        .summary(article.getSummary())
        .content(article.getContent())
        .author(authorInfo)
        .status(article.getStatus())
        .tags(getArticleTags(article.getId()))
        .viewCount(article.getViewCount())
        .reviewComment(article.getReviewComment())
        .reviewer(reviewerInfo)
        .reviewTime(formatDateTime(article.getReviewTime()))
        .publishTime(formatDateTime(article.getPublishTime()))
        .createTime(formatDateTime(article.getCreateTime()))
        .updateTime(formatDateTime(article.getUpdateTime()))
        .build();
  }

  private ArticleListItemResponse toArticleListItemResponse(Article article) {
    User author = userMapper.selectById(article.getAuthorId());

    ArticleListItemResponse.AuthorInfo authorInfo = null;
    if (author != null) {
      authorInfo = ArticleListItemResponse.AuthorInfo.builder()
          .id(author.getId())
          .username(author.getUsername())
          .nickname(author.getNickname())
          .avatar(author.getAvatar())
          .build();
    }

    List<ArticleTagRelation> relations = articleTagRelationMapper.selectList(
        new LambdaQueryWrapper<ArticleTagRelation>()
            .eq(ArticleTagRelation::getArticleId, article.getId())
    );

    List<ArticleListItemResponse.TagInfo> tags = relations.stream()
        .map(r -> {
          ArticleTag tag = articleTagMapper.selectById(r.getTagId());
          if (tag == null) {
            return null;
          }
          return ArticleListItemResponse.TagInfo.builder()
              .id(tag.getId())
              .name(tag.getName())
              .color(tag.getColor())
              .build();
        })
        .filter(t -> t != null)
        .collect(Collectors.toList());

    return ArticleListItemResponse.builder()
        .id(article.getId())
        .title(article.getTitle())
        .summary(article.getSummary())
        .author(authorInfo)
        .status(article.getStatus())
        .tags(tags)
        .viewCount(article.getViewCount())
        .publishTime(formatDateTime(article.getPublishTime()))
        .createTime(formatDateTime(article.getCreateTime()))
        .build();
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
  }

  /**
   * 验证当前用户是否为管理员
   */
  private void verifyAdminRole() {
    User user = getCurrentUser();
    if (!"ADMIN".equals(user.getRole())) {
      throw new BusinessException(403, "无权限执行此操作");
    }
  }

  /**
   * 获取当前登录用户
   */
  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BusinessException(401, "请先登录");
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof UserDetails userDetails)) {
      throw new BusinessException(401, "请先登录");
    }

    if ("anonymousUser".equals(userDetails.getUsername())) {
      throw new BusinessException(401, "请先登录");
    }

    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>()
            .eq(User::getUsername, userDetails.getUsername())
    );

    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    return user;
  }
}
