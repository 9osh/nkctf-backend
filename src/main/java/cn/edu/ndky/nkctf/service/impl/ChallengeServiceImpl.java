package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.ChallengeQueryRequest;
import cn.edu.ndky.nkctf.dto.response.ChallengeDetailResponse;
import cn.edu.ndky.nkctf.dto.response.ChallengeDetailResponse.AttachmentResponse;
import cn.edu.ndky.nkctf.dto.response.ChallengeDetailResponse.HintResponse;
import cn.edu.ndky.nkctf.dto.response.ChallengeListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Hint;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.ChallengeMapper;
import cn.edu.ndky.nkctf.mapper.HintMapper;
import cn.edu.ndky.nkctf.mapper.HintUnlockMapper;
import cn.edu.ndky.nkctf.mapper.SubmissionMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.ChallengeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 挑战服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

  /**
   * 允许的分类值（白名单，大写）
   */
  private static final Set<String> ALLOWED_CATEGORIES = Set.of(
      "WEB", "PWN", "CRYPTO", "REVERSE", "MISC", "BLOCKCHAIN"
  );

  /**
   * 允许的难度值（白名单）
   */
  private static final Set<String> ALLOWED_DIFFICULTIES = Set.of(
      "EASY", "MEDIUM", "HARD"
  );

  /**
   * 允许的排序方式（白名单）
   */
  private static final Set<String> ALLOWED_SORT_OPTIONS = Set.of(
      "newest", "most_solves", "least_solves", "highest_points"
  );

  private final ChallengeMapper challengeMapper;
  private final SubmissionMapper submissionMapper;
  private final HintMapper hintMapper;
  private final HintUnlockMapper hintUnlockMapper;
  private final UserMapper userMapper;

  @Override
  public PageResponse<ChallengeListItemResponse> getChallengeList(ChallengeQueryRequest request) {
    // 构建查询条件（使用 LambdaQueryWrapper 防止 SQL 注入）
    LambdaQueryWrapper<Challenge> queryWrapper = new LambdaQueryWrapper<Challenge>()
        .eq(Challenge::getEnabled, true);

    // 分类过滤（白名单验证，大小写不敏感）
    if (StringUtils.hasText(request.getCategory())) {
      String category = request.getCategory().toUpperCase();
      if (ALLOWED_CATEGORIES.contains(category)) {
        queryWrapper.eq(Challenge::getCategory, category);
      }
    }

    // 难度过滤（白名单验证）
    if (StringUtils.hasText(request.getDifficulty())) {
      String difficulty = request.getDifficulty().toUpperCase();
      if (ALLOWED_DIFFICULTIES.contains(difficulty)) {
        queryWrapper.eq(Challenge::getDifficulty, difficulty);
      }
    }

    // 获取当前用户已解决的题目
    Long currentUserId = getCurrentUserIdOrNull();
    Set<Long> solvedChallengeIds = new HashSet<>();
    if (currentUserId != null) {
      List<Long> solved = submissionMapper.getSolvedChallengeIds(currentUserId);
      if (solved != null) {
        solvedChallengeIds.addAll(solved);
      }
    }

    // 解决状态过滤（需要先查出所有数据再过滤）
    Boolean solvedFilter = request.getSolved();
    boolean needSolvedFilter = solvedFilter != null && currentUserId != null;

    // 预先批量获取解题人数以优化性能
    List<Challenge> challenges = challengeMapper.selectList(queryWrapper);
    Map<Long, Integer> solvesMap = getSolvesMap(challenges);

    // 转换为响应 DTO
    List<ChallengeListItemResponse> responseList = challenges.stream()
        .map(challenge -> {
          Integer solves = solvesMap.getOrDefault(challenge.getId(), 0);
          boolean isSolved = solvedChallengeIds.contains(challenge.getId());
          return ChallengeListItemResponse.builder()
              .id(challenge.getId())
              .title(challenge.getTitle())
              .description(challenge.getDescription())
              .category(challenge.getCategory())
              .difficulty(challenge.getDifficulty())
              .points(challenge.getPoints())
              .solves(solves)
              .solved(isSolved)
              .releaseDate(challenge.getCreateTime() != null
                  ? challenge.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
                  : null)
              .build();
        })
        .collect(Collectors.toList());

    // 应用解决状态过滤
    if (needSolvedFilter) {
      responseList = responseList.stream()
          .filter(item -> solvedFilter.equals(item.getSolved()))
          .collect(Collectors.toList());
    }

    // 应用排序（白名单验证，使用安全的 Comparator）
    String sortBy = request.getSortBy();
    if (StringUtils.hasText(sortBy) && ALLOWED_SORT_OPTIONS.contains(sortBy)) {
      responseList = applySorting(responseList, sortBy);
    } else {
      // 默认排序：最新发布
      responseList.sort(Comparator.comparing(
          ChallengeListItemResponse::getReleaseDate,
          Comparator.nullsLast(Comparator.reverseOrder())
      ));
    }

    // 应用分页
    return applyPagination(responseList, request.getPageNum(), request.getPageSize());
  }

  /**
   * 应用分页
   */
  private PageResponse<ChallengeListItemResponse> applyPagination(
      List<ChallengeListItemResponse> list, int page, int size) {
    long total = list.size();
    int pages = (int) Math.ceil((double) total / size);
    int fromIndex = (page - 1) * size;
    int toIndex = Math.min(fromIndex + size, list.size());

    List<ChallengeListItemResponse> records;
    if (fromIndex >= list.size()) {
      records = new ArrayList<>();
    } else {
      records = list.subList(fromIndex, toIndex);
    }

    return PageResponse.<ChallengeListItemResponse>builder()
        .records(records)
        .total(total)
        .page(page)
        .size(size)
        .pages(pages)
        .hasNext(page < pages)
        .hasPrevious(page > 1)
        .build();
  }

  /**
   * 批量获取解题人数映射
   */
  private Map<Long, Integer> getSolvesMap(List<Challenge> challenges) {
    return challenges.stream()
        .collect(Collectors.toMap(
            Challenge::getId,
            challenge -> {
              Integer count = submissionMapper.countSolvesByChallengeId(challenge.getId());
              return count != null ? count : 0;
            }
        ));
  }

  /**
   * 应用排序（使用安全的 Comparator，不拼接 SQL）
   */
  private List<ChallengeListItemResponse> applySorting(
      List<ChallengeListItemResponse> list, String sortBy) {
    Comparator<ChallengeListItemResponse> comparator = switch (sortBy) {
      case "newest" -> Comparator.comparing(
          ChallengeListItemResponse::getReleaseDate,
          Comparator.nullsLast(Comparator.reverseOrder())
      );
      case "most_solves" -> Comparator.comparing(
          ChallengeListItemResponse::getSolves,
          Comparator.reverseOrder()
      );
      case "least_solves" -> Comparator.comparing(
          ChallengeListItemResponse::getSolves
      );
      case "highest_points" -> Comparator.comparing(
          ChallengeListItemResponse::getPoints,
          Comparator.reverseOrder()
      );
      default -> Comparator.comparing(
          ChallengeListItemResponse::getReleaseDate,
          Comparator.nullsLast(Comparator.reverseOrder())
      );
    };
    return list.stream().sorted(comparator).collect(Collectors.toList());
  }

  @Override
  public ChallengeDetailResponse getChallengeDetail(Long challengeId) {
    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null || !Boolean.TRUE.equals(challenge.getEnabled())) {
      throw new BusinessException(404, "挑战不存在");
    }

    Long currentUserId = getCurrentUserIdOrNull();

    // 获取解题人数
    Integer solves = submissionMapper.countSolvesByChallengeId(challengeId);

    // 检查当前用户是否已解决
    boolean solved = false;
    if (currentUserId != null) {
      solved = Boolean.TRUE.equals(submissionMapper.hasUserSolved(currentUserId, challengeId));
    }

    // 获取提示列表
    List<HintResponse> hints = getHintResponses(challengeId, currentUserId);

    // 获取附件列表
    List<AttachmentResponse> attachments = new ArrayList<>();
    if (StringUtils.hasText(challenge.getAttachmentUrl())) {
      attachments.add(AttachmentResponse.builder()
          .name(challenge.getAttachmentName() != null
              ? challenge.getAttachmentName()
              : "attachment")
          .url(challenge.getAttachmentUrl())
          .build());
    }

    return ChallengeDetailResponse.builder()
        .id(challenge.getId())
        .title(challenge.getTitle())
        .description(challenge.getDescription())
        .category(challenge.getCategory())
        .difficulty(challenge.getDifficulty())
        .points(challenge.getPoints())
        .solves(solves != null ? solves : 0)
        .solved(solved)
        .releaseDate(challenge.getCreateTime() != null
            ? challenge.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
            : null)
        .author(challenge.getAuthor())
        .content(challenge.getContent())
        .hints(hints)
        .attachments(attachments.isEmpty() ? null : attachments)
        .hasDocker(challenge.getDockerImage() != null && !challenge.getDockerImage().isBlank())
        .build();
  }

  private List<HintResponse> getHintResponses(Long challengeId, Long userId) {
    List<Hint> hints = hintMapper.findByChallengeId(challengeId);
    if (hints == null || hints.isEmpty()) {
      return null;
    }

    // 获取用户已解锁的提示
    Set<Long> unlockedHintIds = new HashSet<>();
    if (userId != null) {
      List<Long> unlocked = hintUnlockMapper.getUnlockedHintIds(userId);
      if (unlocked != null) {
        unlockedHintIds.addAll(unlocked);
      }
    }

    return hints.stream()
        .map(hint -> {
          boolean unlocked = unlockedHintIds.contains(hint.getId());
          return HintResponse.builder()
              .id(hint.getId())
              .cost(hint.getCost())
              .unlocked(unlocked)
              .content(unlocked ? hint.getContent() : null)
              .build();
        })
        .collect(Collectors.toList());
  }

  /**
   * 获取当前用户 ID，未登录返回 null
   */
  private Long getCurrentUserIdOrNull() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      // 排除匿名用户
      if ("anonymousUser".equals(userDetails.getUsername())) {
        return null;
      }
      User user = userMapper.selectOne(
          new LambdaQueryWrapper<User>()
              .eq(User::getUsername, userDetails.getUsername())
      );
      return user != null ? user.getId() : null;
    }

    return null;
  }
}
