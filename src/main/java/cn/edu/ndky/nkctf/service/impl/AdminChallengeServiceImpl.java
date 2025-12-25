package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;
import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Hint;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.HintMapper;
import cn.edu.ndky.nkctf.mapper.SubmissionMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.AdminChallengeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理员题目管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChallengeServiceImpl implements AdminChallengeService {

  private final ChallengeMapper challengeMapper;
  private final HintMapper hintMapper;
  private final SubmissionMapper submissionMapper;
  private final UserMapper userMapper;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Set<String> ALLOWED_CATEGORIES =
      Set.of("WEB", "PWN", "CRYPTO", "REVERSE", "MISC", "BLOCKCHAIN");

  private static final Set<String> ALLOWED_DIFFICULTIES =
      Set.of("EASY", "MEDIUM", "HARD");

  @Value("${app.upload.path:./uploads}")
  private String uploadPath;

  @Value("${app.upload.attachment-url-prefix:/attachments/download}")
  private String attachmentUrlPrefix;

  // ========== 题目 CRUD ==========

  @Override
  public PageResponse<AdminChallengeListItemResponse> getChallengeList(
      AdminChallengeQueryRequest request) {
    LambdaQueryWrapper<Challenge> wrapper = new LambdaQueryWrapper<>();

    // 分类过滤
    if (StringUtils.hasText(request.getCategory())) {
      String category = request.getCategory().toUpperCase();
      if (ALLOWED_CATEGORIES.contains(category)) {
        wrapper.eq(Challenge::getCategory, category);
      }
    }

    // 难度过滤
    if (StringUtils.hasText(request.getDifficulty())) {
      String difficulty = request.getDifficulty().toUpperCase();
      if (ALLOWED_DIFFICULTIES.contains(difficulty)) {
        wrapper.eq(Challenge::getDifficulty, difficulty);
      }
    }

    // 启用状态过滤
    if (request.getEnabled() != null) {
      wrapper.eq(Challenge::getEnabled, request.getEnabled());
    }

    // 关键词搜索
    if (StringUtils.hasText(request.getKeyword())) {
      wrapper.like(Challenge::getTitle, request.getKeyword());
    }

    // 按创建时间降序
    wrapper.orderByDesc(Challenge::getCreateTime);

    // 分页查询
    Page<Challenge> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<Challenge> result = challengeMapper.selectPage(page, wrapper);

    List<AdminChallengeListItemResponse> items = result.getRecords().stream()
        .map(this::toAdminChallengeListItemResponse)
        .collect(Collectors.toList());

    return PageResponse.<AdminChallengeListItemResponse>builder()
        .records(items)
        .total(result.getTotal())
        .page((int) result.getCurrent())
        .size((int) result.getSize())
        .pages((int) result.getPages())
        .hasNext(result.hasNext())
        .hasPrevious(result.hasPrevious())
        .build();
  }

  @Override
  public AdminChallengeDetailResponse getChallengeDetail(Long challengeId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }
    return toAdminChallengeDetailResponse(challenge);
  }

  @Override
  @Transactional
  public AdminChallengeDetailResponse createChallenge(CreateChallengeRequest request) {
    // 验证分类
    String category = request.getCategory().toUpperCase();
    if (!ALLOWED_CATEGORIES.contains(category)) {
      throw new BusinessException(400, "无效的分类: " + request.getCategory());
    }

    // 验证难度
    String difficulty = request.getDifficulty().toUpperCase();
    if (!ALLOWED_DIFFICULTIES.contains(difficulty)) {
      throw new BusinessException(400, "无效的难度: " + request.getDifficulty());
    }

    // 验证动态题目配置
    Boolean isDynamic = request.getIsDynamic() != null && request.getIsDynamic();
    if (isDynamic) {
      if (!StringUtils.hasText(request.getDockerImage())) {
        throw new BusinessException(400, "动态题目必须指定 Docker 镜像");
      }
    } else {
      if (!StringUtils.hasText(request.getFlag())) {
        throw new BusinessException(400, "静态题目必须指定 Flag");
      }
    }

    Challenge challenge = new Challenge();
    challenge.setTitle(request.getTitle());
    challenge.setDescription(request.getDescription());
    challenge.setContent(request.getContent());
    challenge.setCategory(category);
    challenge.setDifficulty(difficulty);
    challenge.setPoints(request.getPoints());
    challenge.setAuthor(request.getAuthor());
    challenge.setFlag(request.getFlag());
    challenge.setIsDynamic(isDynamic);
    challenge.setDockerImage(request.getDockerImage());
    challenge.setEnabled(request.getEnabled() != null ? request.getEnabled() : false);

    challengeMapper.insert(challenge);

    User admin = getCurrentUser();
    log.info("管理员 {} 创建了题目: {}", admin.getUsername(), challenge.getTitle());

    return toAdminChallengeDetailResponse(challenge);
  }

  @Override
  @Transactional
  public AdminChallengeDetailResponse updateChallenge(Long challengeId,
      UpdateChallengeRequest request) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // 更新标题
    if (StringUtils.hasText(request.getTitle())) {
      challenge.setTitle(request.getTitle());
    }

    // 更新描述
    if (request.getDescription() != null) {
      challenge.setDescription(request.getDescription());
    }

    // 更新内容
    if (request.getContent() != null) {
      challenge.setContent(request.getContent());
    }

    // 更新分类
    if (StringUtils.hasText(request.getCategory())) {
      String category = request.getCategory().toUpperCase();
      if (!ALLOWED_CATEGORIES.contains(category)) {
        throw new BusinessException(400, "无效的分类: " + request.getCategory());
      }
      challenge.setCategory(category);
    }

    // 更新难度
    if (StringUtils.hasText(request.getDifficulty())) {
      String difficulty = request.getDifficulty().toUpperCase();
      if (!ALLOWED_DIFFICULTIES.contains(difficulty)) {
        throw new BusinessException(400, "无效的难度: " + request.getDifficulty());
      }
      challenge.setDifficulty(difficulty);
    }

    // 更新分数
    if (request.getPoints() != null) {
      challenge.setPoints(request.getPoints());
    }

    // 更新作者
    if (request.getAuthor() != null) {
      challenge.setAuthor(request.getAuthor());
    }

    // 更新 Flag
    if (request.getFlag() != null) {
      challenge.setFlag(request.getFlag());
    }

    // 更新动态题目配置
    if (request.getIsDynamic() != null) {
      challenge.setIsDynamic(request.getIsDynamic());
    }

    // 更新 Docker 镜像
    if (request.getDockerImage() != null) {
      challenge.setDockerImage(request.getDockerImage());
    }

    // 更新启用状态
    if (request.getEnabled() != null) {
      challenge.setEnabled(request.getEnabled());
    }

    // 验证动态题目配置
    if (Boolean.TRUE.equals(challenge.getIsDynamic())) {
      if (!StringUtils.hasText(challenge.getDockerImage())) {
        throw new BusinessException(400, "动态题目必须指定 Docker 镜像");
      }
    } else {
      if (!StringUtils.hasText(challenge.getFlag())) {
        throw new BusinessException(400, "静态题目必须指定 Flag");
      }
    }

    challengeMapper.updateById(challenge);

    User admin = getCurrentUser();
    log.info("管理员 {} 更新了题目: {}", admin.getUsername(), challenge.getTitle());

    return toAdminChallengeDetailResponse(challenge);
  }

  @Override
  @Transactional
  public void deleteChallenge(Long challengeId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // 删除相关提示
    hintMapper.delete(new LambdaQueryWrapper<Hint>()
        .eq(Hint::getChallengeId, challengeId));

    // 软删除题目
    challengeMapper.deleteById(challengeId);

    User admin = getCurrentUser();
    log.info("管理员 {} 删除了题目: {}", admin.getUsername(), challenge.getTitle());
  }

  @Override
  @Transactional
  public void toggleEnabled(Long challengeId, ToggleEnabledRequest request) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    challenge.setEnabled(request.getEnabled());
    challengeMapper.updateById(challenge);

    User admin = getCurrentUser();
    String action = request.getEnabled() ? "启用" : "禁用";
    log.info("管理员 {} {}了题目: {}", admin.getUsername(), action, challenge.getTitle());
  }

  // ========== 附件管理 ==========

  @Override
  @Transactional
  public AttachmentUploadResponse uploadAttachment(Long challengeId, MultipartFile file) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    if (file.isEmpty()) {
      throw new BusinessException(400, "文件不能为空");
    }

    // 生成唯一文件名
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String newFilename = UUID.randomUUID().toString() + extension;

    // 保存文件
    try {
      Path uploadDir = Paths.get(uploadPath, "attachments");
      Files.createDirectories(uploadDir);
      Path filePath = uploadDir.resolve(newFilename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("文件上传失败", e);
      throw new BusinessException(500, "文件上传失败");
    }

    // 删除旧附件
    if (StringUtils.hasText(challenge.getAttachmentUrl())) {
      deleteAttachmentFile(challenge.getAttachmentUrl());
    }

    // 更新题目附件信息
    String attachmentUrl = attachmentUrlPrefix + "/" + newFilename;
    challenge.setAttachmentUrl(attachmentUrl);
    challenge.setAttachmentName(originalFilename);
    challengeMapper.updateById(challenge);

    User admin = getCurrentUser();
    log.info("管理员 {} 为题目 {} 上传了附件: {}",
        admin.getUsername(), challenge.getTitle(), originalFilename);

    return AttachmentUploadResponse.builder()
        .url(attachmentUrl)
        .name(originalFilename)
        .build();
  }

  @Override
  @Transactional
  public void deleteAttachment(Long challengeId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    if (!StringUtils.hasText(challenge.getAttachmentUrl())) {
      throw new BusinessException(400, "题目没有附件");
    }

    // 删除文件
    deleteAttachmentFile(challenge.getAttachmentUrl());

    // 清除附件信息
    challenge.setAttachmentUrl(null);
    challenge.setAttachmentName(null);
    challengeMapper.updateById(challenge);

    User admin = getCurrentUser();
    log.info("管理员 {} 删除了题目 {} 的附件", admin.getUsername(), challenge.getTitle());
  }

  // ========== 提示管理 ==========

  @Override
  public List<AdminHintResponse> getHints(Long challengeId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    List<Hint> hints = hintMapper.findByChallengeId(challengeId);
    return hints.stream()
        .map(this::toAdminHintResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public AdminHintResponse createHint(Long challengeId, CreateHintRequest request) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    // 自动分配排序顺序
    Integer sortOrder = request.getSortOrder();
    if (sortOrder == null) {
      Long count = hintMapper.selectCount(
          new LambdaQueryWrapper<Hint>().eq(Hint::getChallengeId, challengeId));
      sortOrder = count.intValue() + 1;
    }

    Hint hint = new Hint();
    hint.setChallengeId(challengeId);
    hint.setContent(request.getContent());
    hint.setCost(request.getCost());
    hint.setSortOrder(sortOrder);

    hintMapper.insert(hint);

    User admin = getCurrentUser();
    log.info("管理员 {} 为题目 {} 创建了提示", admin.getUsername(), challenge.getTitle());

    return toAdminHintResponse(hint);
  }

  @Override
  @Transactional
  public AdminHintResponse updateHint(Long challengeId, Long hintId, UpdateHintRequest request) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    Hint hint = hintMapper.selectById(hintId);
    if (hint == null || !hint.getChallengeId().equals(challengeId)) {
      throw new BusinessException(404, "提示不存在");
    }

    if (StringUtils.hasText(request.getContent())) {
      hint.setContent(request.getContent());
    }
    if (request.getCost() != null) {
      hint.setCost(request.getCost());
    }
    if (request.getSortOrder() != null) {
      hint.setSortOrder(request.getSortOrder());
    }

    hintMapper.updateById(hint);

    User admin = getCurrentUser();
    log.info("管理员 {} 更新了题目 {} 的提示", admin.getUsername(), challenge.getTitle());

    return toAdminHintResponse(hint);
  }

  @Override
  @Transactional
  public void deleteHint(Long challengeId, Long hintId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    Hint hint = hintMapper.selectById(hintId);
    if (hint == null || !hint.getChallengeId().equals(challengeId)) {
      throw new BusinessException(404, "提示不存在");
    }

    hintMapper.deleteById(hintId);

    User admin = getCurrentUser();
    log.info("管理员 {} 删除了题目 {} 的提示", admin.getUsername(), challenge.getTitle());
  }

  @Override
  @Transactional
  public List<AdminHintResponse> reorderHints(Long challengeId, ReorderHintsRequest request) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      throw new BusinessException(404, "题目不存在");
    }

    List<Hint> existingHints = hintMapper.findByChallengeId(challengeId);
    Set<Long> existingIds = existingHints.stream()
        .map(Hint::getId)
        .collect(Collectors.toSet());

    // 验证所有 ID 都属于此题目
    for (Long hintId : request.getHintIds()) {
      if (!existingIds.contains(hintId)) {
        throw new BusinessException(400, "提示 ID " + hintId + " 不属于此题目");
      }
    }

    // 更新排序顺序
    int order = 1;
    for (Long hintId : request.getHintIds()) {
      Hint hint = hintMapper.selectById(hintId);
      hint.setSortOrder(order++);
      hintMapper.updateById(hint);
    }

    User admin = getCurrentUser();
    log.info("管理员 {} 重排序了题目 {} 的提示", admin.getUsername(), challenge.getTitle());

    return getHints(challengeId);
  }

  // ========== 私有方法 ==========

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

  private void deleteAttachmentFile(String attachmentUrl) {
    if (!StringUtils.hasText(attachmentUrl)) {
      return;
    }

    try {
      // 从 URL 提取文件名
      String filename = attachmentUrl.substring(attachmentUrl.lastIndexOf("/") + 1);
      Path filePath = Paths.get(uploadPath, "attachments", filename);
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      log.warn("删除附件文件失败: {}", attachmentUrl, e);
    }
  }

  private AdminChallengeListItemResponse toAdminChallengeListItemResponse(Challenge challenge) {
    Integer solves = submissionMapper.countSolvesByChallengeId(challenge.getId());

    return AdminChallengeListItemResponse.builder()
        .id(challenge.getId())
        .title(challenge.getTitle())
        .description(challenge.getDescription())
        .category(challenge.getCategory())
        .difficulty(challenge.getDifficulty())
        .points(challenge.getPoints())
        .author(challenge.getAuthor())
        .isDynamic(challenge.getIsDynamic())
        .enabled(challenge.getEnabled())
        .solves(solves != null ? solves : 0)
        .createTime(challenge.getCreateTime() != null ?
            challenge.getCreateTime().format(DATE_FORMATTER) : null)
        .updateTime(challenge.getUpdateTime() != null ?
            challenge.getUpdateTime().format(DATE_FORMATTER) : null)
        .build();
  }

  private AdminChallengeDetailResponse toAdminChallengeDetailResponse(Challenge challenge) {
    Integer solves = submissionMapper.countSolvesByChallengeId(challenge.getId());
    List<Hint> hints = hintMapper.findByChallengeId(challenge.getId());
    List<AdminHintResponse> hintResponses = hints.stream()
        .map(this::toAdminHintResponse)
        .collect(Collectors.toList());

    return AdminChallengeDetailResponse.builder()
        .id(challenge.getId())
        .title(challenge.getTitle())
        .description(challenge.getDescription())
        .content(challenge.getContent())
        .category(challenge.getCategory())
        .difficulty(challenge.getDifficulty())
        .points(challenge.getPoints())
        .author(challenge.getAuthor())
        .flag(challenge.getFlag())
        .isDynamic(challenge.getIsDynamic())
        .dockerImage(challenge.getDockerImage())
        .attachmentUrl(challenge.getAttachmentUrl())
        .attachmentName(challenge.getAttachmentName())
        .enabled(challenge.getEnabled())
        .solves(solves != null ? solves : 0)
        .hints(hintResponses)
        .createTime(challenge.getCreateTime() != null ?
            challenge.getCreateTime().format(DATE_FORMATTER) : null)
        .updateTime(challenge.getUpdateTime() != null ?
            challenge.getUpdateTime().format(DATE_FORMATTER) : null)
        .build();
  }

  private AdminHintResponse toAdminHintResponse(Hint hint) {
    return AdminHintResponse.builder()
        .id(hint.getId())
        .challengeId(hint.getChallengeId())
        .content(hint.getContent())
        .cost(hint.getCost())
        .sortOrder(hint.getSortOrder())
        .createTime(hint.getCreateTime() != null ?
            hint.getCreateTime().format(DATE_FORMATTER) : null)
        .build();
  }
}
