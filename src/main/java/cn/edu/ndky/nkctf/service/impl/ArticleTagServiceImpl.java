package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.CreateTagRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateTagRequest;
import cn.edu.ndky.nkctf.dto.response.TagResponse;
import cn.edu.ndky.nkctf.entity.ArticleTag;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ArticleTagMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.ArticleTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleTagServiceImpl implements ArticleTagService {

  private final ArticleTagMapper articleTagMapper;
  private final UserMapper userMapper;

  @Override
  public List<TagResponse> getAllTags() {
    List<ArticleTag> tags = articleTagMapper.selectList(
        new LambdaQueryWrapper<ArticleTag>()
            .orderByAsc(ArticleTag::getSortOrder)
            .orderByAsc(ArticleTag::getId)
    );

    return tags.stream()
        .map(this::toTagResponse)
        .collect(Collectors.toList());
  }

  @Override
  public TagResponse createTag(CreateTagRequest request) {
    verifyAdminRole();

    // 检查标签名是否已存在
    ArticleTag existing = articleTagMapper.selectOne(
        new LambdaQueryWrapper<ArticleTag>()
            .eq(ArticleTag::getName, request.getName())
    );
    if (existing != null) {
      throw new BusinessException(400, "标签名称已存在");
    }

    ArticleTag tag = new ArticleTag();
    tag.setName(request.getName());
    tag.setDescription(request.getDescription());
    tag.setColor(request.getColor() != null ? request.getColor() : "#3B82F6");
    tag.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

    articleTagMapper.insert(tag);

    log.info("管理员创建了标签: {}", tag.getName());

    return toTagResponse(tag);
  }

  @Override
  public TagResponse updateTag(UpdateTagRequest request) {
    verifyAdminRole();

    ArticleTag tag = articleTagMapper.selectById(request.getId());
    if (tag == null) {
      throw new BusinessException(404, "标签不存在");
    }

    // 检查新名称是否与其他标签冲突
    ArticleTag existing = articleTagMapper.selectOne(
        new LambdaQueryWrapper<ArticleTag>()
            .eq(ArticleTag::getName, request.getName())
            .ne(ArticleTag::getId, request.getId())
    );
    if (existing != null) {
      throw new BusinessException(400, "标签名称已存在");
    }

    tag.setName(request.getName());
    tag.setDescription(request.getDescription());
    if (request.getColor() != null) {
      tag.setColor(request.getColor());
    }
    if (request.getSortOrder() != null) {
      tag.setSortOrder(request.getSortOrder());
    }

    articleTagMapper.updateById(tag);

    log.info("管理员更新了标签: {}", tag.getName());

    return toTagResponse(tag);
  }

  @Override
  public void deleteTag(Long tagId) {
    verifyAdminRole();

    ArticleTag tag = articleTagMapper.selectById(tagId);
    if (tag == null) {
      throw new BusinessException(404, "标签不存在");
    }

    articleTagMapper.deleteById(tagId);

    log.info("管理员删除了标签: {}", tag.getName());
  }

  private TagResponse toTagResponse(ArticleTag tag) {
    return TagResponse.builder()
        .id(tag.getId())
        .name(tag.getName())
        .description(tag.getDescription())
        .color(tag.getColor())
        .sortOrder(tag.getSortOrder())
        .articleCount(articleTagMapper.countPublishedArticlesByTag(tag.getId()))
        .build();
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
