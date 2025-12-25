package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.*;
import cn.edu.ndky.nkctf.dto.response.*;
import cn.edu.ndky.nkctf.entity.*;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.*;
import cn.edu.ndky.nkctf.service.AdminCompetitionService;
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员竞赛管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCompetitionServiceImpl implements AdminCompetitionService {

  private final CompetitionMapper competitionMapper;
  private final CompetitionChallengeMapper competitionChallengeMapper;
  private final CompetitionUserMapper competitionUserMapper;
  private final CompetitionTeamMapper competitionTeamMapper;
  private final ChallengeMapper challengeMapper;
  private final SubmissionMapper submissionMapper;
  private final UserMapper userMapper;
  private final TeamMapper teamMapper;
  private final TeamMemberMapper teamMemberMapper;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Set<String> ALLOWED_STATUSES =
      Set.of("inactive", "active", "ending");

  // ========== 竞赛 CRUD ==========

  @Override
  public PageResponse<AdminCompetitionListItemResponse> getCompetitionList(
      AdminCompetitionQueryRequest request) {
    LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();

    // 状态过滤
    if (StringUtils.hasText(request.getStatus())) {
      String status = request.getStatus().toLowerCase();
      if (ALLOWED_STATUSES.contains(status)) {
        wrapper.eq(Competition::getStatus, status);
      }
    }

    // 关键词搜索
    if (StringUtils.hasText(request.getKeyword())) {
      wrapper.like(Competition::getName, request.getKeyword());
    }

    // 按创建时间降序
    wrapper.orderByDesc(Competition::getCreateTime);

    // 分页查询
    Page<Competition> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<Competition> result = competitionMapper.selectPage(page, wrapper);

    List<AdminCompetitionListItemResponse> items = result.getRecords().stream()
        .map(this::toAdminCompetitionListItemResponse)
        .collect(Collectors.toList());

    return PageResponse.<AdminCompetitionListItemResponse>builder()
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
  public AdminCompetitionDetailResponse getCompetitionDetail(Long competitionId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }
    return toAdminCompetitionDetailResponse(competition);
  }

  @Override
  @Transactional
  public AdminCompetitionDetailResponse createCompetition(CreateCompetitionRequest request) {
    // 验证名称唯一性
    Long count = competitionMapper.selectCount(
        new LambdaQueryWrapper<Competition>()
            .eq(Competition::getName, request.getName())
    );
    if (count > 0) {
      throw new BusinessException(400, "竞赛名称已存在");
    }

    // 验证时间
    if (request.getStartTime().isAfter(request.getEndTime())) {
      throw new BusinessException(400, "开始时间不能晚于结束时间");
    }

    Competition competition = new Competition();
    competition.setName(request.getName());
    competition.setDescription(request.getDescription());
    competition.setIsTeamCompetition(request.getIsTeamCompetition());
    competition.setStartTime(request.getStartTime());
    competition.setEndTime(request.getEndTime());
    competition.setStatus(Competition.Status.INACTIVE.getValue());

    competitionMapper.insert(competition);

    User admin = getCurrentUser();
    log.info("管理员 {} 创建了竞赛: {}", admin.getUsername(), competition.getName());

    return toAdminCompetitionDetailResponse(competition);
  }

  @Override
  @Transactional
  public AdminCompetitionDetailResponse updateCompetition(Long competitionId,
      UpdateCompetitionRequest request) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 更新名称
    if (StringUtils.hasText(request.getName())) {
      // 检查名称唯一性
      Long count = competitionMapper.selectCount(
          new LambdaQueryWrapper<Competition>()
              .eq(Competition::getName, request.getName())
              .ne(Competition::getId, competitionId)
      );
      if (count > 0) {
        throw new BusinessException(400, "竞赛名称已存在");
      }
      competition.setName(request.getName());
    }

    // 更新描述
    if (request.getDescription() != null) {
      competition.setDescription(request.getDescription());
    }

    // 更新竞赛类型（仅 INACTIVE 状态可修改）
    if (request.getIsTeamCompetition() != null) {
      if (!"inactive".equals(competition.getStatus())) {
        throw new BusinessException(400, "竞赛进行中或已结束，无法修改竞赛类型");
      }
      competition.setIsTeamCompetition(request.getIsTeamCompetition());
    }

    // 更新时间
    if (request.getStartTime() != null) {
      competition.setStartTime(request.getStartTime());
    }
    if (request.getEndTime() != null) {
      competition.setEndTime(request.getEndTime());
    }

    // 验证时间
    if (competition.getStartTime().isAfter(competition.getEndTime())) {
      throw new BusinessException(400, "开始时间不能晚于结束时间");
    }

    competitionMapper.updateById(competition);

    User admin = getCurrentUser();
    log.info("管理员 {} 更新了竞赛: {}", admin.getUsername(), competition.getName());

    return toAdminCompetitionDetailResponse(competition);
  }

  @Override
  @Transactional
  public void deleteCompetition(Long competitionId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 删除竞赛题目关联
    competitionChallengeMapper.delete(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competitionId)
    );

    // 删除竞赛参赛者关联
    competitionUserMapper.delete(
        new LambdaQueryWrapper<CompetitionUser>()
            .eq(CompetitionUser::getCompetitionId, competitionId)
    );
    competitionTeamMapper.delete(
        new LambdaQueryWrapper<CompetitionTeam>()
            .eq(CompetitionTeam::getCompetitionId, competitionId)
    );

    // 软删除竞赛
    competitionMapper.deleteById(competitionId);

    User admin = getCurrentUser();
    log.info("管理员 {} 删除了竞赛: {}", admin.getUsername(), competition.getName());
  }

  @Override
  @Transactional
  public void changeStatus(Long competitionId, ChangeStatusRequest request) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    String newStatus = request.getStatus().toLowerCase();
    if (!ALLOWED_STATUSES.contains(newStatus)) {
      throw new BusinessException(400, "无效的状态: " + request.getStatus());
    }

    competition.setStatus(newStatus);
    competitionMapper.updateById(competition);

    User admin = getCurrentUser();
    log.info("管理员 {} 将竞赛 {} 的状态更改为 {}",
        admin.getUsername(), competition.getName(), newStatus);
  }

  // ========== 题目管理 ==========

  @Override
  public List<AdminCompetitionChallengeResponse> getChallenges(Long competitionId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    List<CompetitionChallenge> relations = competitionChallengeMapper.selectList(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competitionId)
            .orderByAsc(CompetitionChallenge::getSortOrder)
            .orderByAsc(CompetitionChallenge::getId)
    );

    return relations.stream()
        .map(rel -> toAdminCompetitionChallengeResponse(rel, competition))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public List<AdminCompetitionChallengeResponse> addChallenges(Long competitionId,
      AddChallengesRequest request) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 获取当前最大排序号
    Long maxOrder = competitionChallengeMapper.selectCount(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competitionId)
    );
    int sortOrder = maxOrder.intValue() + 1;

    for (Long challengeId : request.getChallengeIds()) {
      // 验证题目存在
      Challenge challenge = challengeMapper.selectById(challengeId);
      if (challenge == null) {
        throw new BusinessException(400, "题目 " + challengeId + " 不存在");
      }

      // 检查是否已添加
      Boolean exists = competitionChallengeMapper.isChallengeInCompetition(
          competitionId, challengeId);
      if (Boolean.TRUE.equals(exists)) {
        continue; // 跳过已添加的题目
      }

      CompetitionChallenge relation = new CompetitionChallenge();
      relation.setCompetitionId(competitionId);
      relation.setChallengeId(challengeId);
      relation.setSortOrder(sortOrder++);
      competitionChallengeMapper.insert(relation);
    }

    User admin = getCurrentUser();
    log.info("管理员 {} 为竞赛 {} 添加了 {} 道题目",
        admin.getUsername(), competition.getName(), request.getChallengeIds().size());

    return getChallenges(competitionId);
  }

  @Override
  @Transactional
  public void removeChallenge(Long competitionId, Long challengeId) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    int deleted = competitionChallengeMapper.delete(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competitionId)
            .eq(CompetitionChallenge::getChallengeId, challengeId)
    );

    if (deleted == 0) {
      throw new BusinessException(404, "竞赛中不存在此题目");
    }

    User admin = getCurrentUser();
    log.info("管理员 {} 从竞赛 {} 移除了题目 {}",
        admin.getUsername(), competition.getName(), challengeId);
  }

  @Override
  @Transactional
  public List<AdminCompetitionChallengeResponse> reorderChallenges(Long competitionId,
      ReorderChallengesRequest request) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    // 获取当前题目
    List<CompetitionChallenge> relations = competitionChallengeMapper.selectList(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competitionId)
    );
    Set<Long> existingChallengeIds = relations.stream()
        .map(CompetitionChallenge::getChallengeId)
        .collect(Collectors.toSet());

    // 验证所有题目都属于此竞赛
    for (Long challengeId : request.getChallengeIds()) {
      if (!existingChallengeIds.contains(challengeId)) {
        throw new BusinessException(400, "题目 " + challengeId + " 不在此竞赛中");
      }
    }

    // 更新排序
    int order = 1;
    for (Long challengeId : request.getChallengeIds()) {
      CompetitionChallenge relation = competitionChallengeMapper.selectOne(
          new LambdaQueryWrapper<CompetitionChallenge>()
              .eq(CompetitionChallenge::getCompetitionId, competitionId)
              .eq(CompetitionChallenge::getChallengeId, challengeId)
      );
      if (relation != null) {
        relation.setSortOrder(order++);
        competitionChallengeMapper.updateById(relation);
      }
    }

    User admin = getCurrentUser();
    log.info("管理员 {} 重排序了竞赛 {} 的题目", admin.getUsername(), competition.getName());

    return getChallenges(competitionId);
  }

  // ========== 参赛者管理 ==========

  @Override
  public PageResponse<AdminParticipantResponse> getParticipants(Long competitionId,
      AdminParticipantQueryRequest request) {
    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      throw new BusinessException(404, "竞赛不存在");
    }

    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      return getTeamParticipants(competition, request);
    } else {
      return getUserParticipants(competition, request);
    }
  }

  // ========== 私有方法 ==========

  private PageResponse<AdminParticipantResponse> getUserParticipants(Competition competition,
      AdminParticipantQueryRequest request) {
    LambdaQueryWrapper<CompetitionUser> wrapper = new LambdaQueryWrapper<CompetitionUser>()
        .eq(CompetitionUser::getCompetitionId, competition.getId())
        .orderByDesc(CompetitionUser::getScore)
        .orderByAsc(CompetitionUser::getCreateTime);

    Page<CompetitionUser> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<CompetitionUser> result = competitionUserMapper.selectPage(page, wrapper);

    List<AdminParticipantResponse> items = new ArrayList<>();
    for (CompetitionUser cu : result.getRecords()) {
      User user = userMapper.selectById(cu.getUserId());
      if (user == null) continue;

      // 关键词过滤
      if (StringUtils.hasText(request.getKeyword())) {
        if (!user.getUsername().contains(request.getKeyword()) &&
            (user.getNickname() == null ||
                !user.getNickname().contains(request.getKeyword()))) {
          continue;
        }
      }

      // 统计解题数
      Integer solvedCount = countUserSolves(competition.getId(), cu.getUserId());

      items.add(AdminParticipantResponse.builder()
          .id(cu.getId())
          .participantId(user.getId().toString())
          .name(user.getNickname() != null ? user.getNickname() : user.getUsername())
          .type("user")
          .score(cu.getScore() != null ? cu.getScore() : 0)
          .solvedCount(solvedCount)
          .registerTime(cu.getCreateTime() != null ?
              cu.getCreateTime().format(DATE_FORMATTER) : null)
          .memberCount(null)
          .captainName(null)
          .build());
    }

    return PageResponse.<AdminParticipantResponse>builder()
        .records(items)
        .total(result.getTotal())
        .page((int) result.getCurrent())
        .size((int) result.getSize())
        .pages((int) result.getPages())
        .hasNext(result.hasNext())
        .hasPrevious(result.hasPrevious())
        .build();
  }

  private PageResponse<AdminParticipantResponse> getTeamParticipants(Competition competition,
      AdminParticipantQueryRequest request) {
    LambdaQueryWrapper<CompetitionTeam> wrapper = new LambdaQueryWrapper<CompetitionTeam>()
        .eq(CompetitionTeam::getCompetitionId, competition.getId())
        .orderByDesc(CompetitionTeam::getScore)
        .orderByAsc(CompetitionTeam::getCreateTime);

    Page<CompetitionTeam> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<CompetitionTeam> result = competitionTeamMapper.selectPage(page, wrapper);

    List<AdminParticipantResponse> items = new ArrayList<>();
    for (CompetitionTeam ct : result.getRecords()) {
      Team team = teamMapper.selectById(ct.getTeamId());
      if (team == null) continue;

      // 关键词过滤
      if (StringUtils.hasText(request.getKeyword())) {
        if (!team.getName().contains(request.getKeyword())) {
          continue;
        }
      }

      // 获取队长信息
      User captain = userMapper.selectById(team.getCaptainId());
      String captainName = captain != null ?
          (captain.getNickname() != null ? captain.getNickname() : captain.getUsername()) : null;

      // 统计成员数和解题数
      Long memberCount = teamMemberMapper.selectCount(
          new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, ct.getTeamId())
      );
      Integer solvedCount = countTeamSolves(competition.getId(), ct.getTeamId());

      items.add(AdminParticipantResponse.builder()
          .id(ct.getId())
          .participantId(ct.getTeamId().toString())
          .name(team.getName())
          .type("team")
          .score(ct.getScore() != null ? ct.getScore() : 0)
          .solvedCount(solvedCount)
          .registerTime(ct.getCreateTime() != null ?
              ct.getCreateTime().format(DATE_FORMATTER) : null)
          .memberCount(memberCount.intValue())
          .captainName(captainName)
          .build());
    }

    return PageResponse.<AdminParticipantResponse>builder()
        .records(items)
        .total(result.getTotal())
        .page((int) result.getCurrent())
        .size((int) result.getSize())
        .pages((int) result.getPages())
        .hasNext(result.hasNext())
        .hasPrevious(result.hasPrevious())
        .build();
  }

  private Integer countUserSolves(Long competitionId, Long userId) {
    List<Long> solvedIds = submissionMapper.getUserSolvedChallengeIdsInCompetition(
        competitionId, userId);
    return solvedIds != null ? solvedIds.size() : 0;
  }

  private Integer countTeamSolves(Long competitionId, UUID teamId) {
    List<Long> solvedIds = submissionMapper.getTeamSolvedChallengeIdsInCompetition(
        competitionId, teamId);
    return solvedIds != null ? solvedIds.size() : 0;
  }

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

  private AdminCompetitionListItemResponse toAdminCompetitionListItemResponse(
      Competition competition) {
    // 统计参赛人数/团队数
    Integer participantCount;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      participantCount = competitionTeamMapper.countTeamsByCompetition(competition.getId());
    } else {
      participantCount = competitionUserMapper.countUsersByCompetition(competition.getId());
    }

    // 统计题目数
    Long challengeCount = competitionChallengeMapper.selectCount(
        new LambdaQueryWrapper<CompetitionChallenge>()
            .eq(CompetitionChallenge::getCompetitionId, competition.getId())
    );

    return AdminCompetitionListItemResponse.builder()
        .id(competition.getId())
        .name(competition.getName())
        .description(competition.getDescription())
        .isTeamCompetition(competition.getIsTeamCompetition())
        .status(competition.getStatus())
        .startTime(competition.getStartTime() != null ?
            competition.getStartTime().format(DATE_FORMATTER) : null)
        .endTime(competition.getEndTime() != null ?
            competition.getEndTime().format(DATE_FORMATTER) : null)
        .participantCount(participantCount != null ? participantCount : 0)
        .challengeCount(challengeCount.intValue())
        .createTime(competition.getCreateTime() != null ?
            competition.getCreateTime().format(DATE_FORMATTER) : null)
        .build();
  }

  private AdminCompetitionDetailResponse toAdminCompetitionDetailResponse(Competition competition) {
    // 统计参赛人数/团队数
    Integer participantCount;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      participantCount = competitionTeamMapper.countTeamsByCompetition(competition.getId());
    } else {
      participantCount = competitionUserMapper.countUsersByCompetition(competition.getId());
    }

    // 获取题目列表
    List<AdminCompetitionChallengeResponse> challenges = getChallenges(competition.getId());

    return AdminCompetitionDetailResponse.builder()
        .id(competition.getId())
        .name(competition.getName())
        .description(competition.getDescription())
        .isTeamCompetition(competition.getIsTeamCompetition())
        .status(competition.getStatus())
        .startTime(competition.getStartTime() != null ?
            competition.getStartTime().format(DATE_FORMATTER) : null)
        .endTime(competition.getEndTime() != null ?
            competition.getEndTime().format(DATE_FORMATTER) : null)
        .participantCount(participantCount != null ? participantCount : 0)
        .challenges(challenges)
        .createTime(competition.getCreateTime() != null ?
            competition.getCreateTime().format(DATE_FORMATTER) : null)
        .updateTime(competition.getUpdateTime() != null ?
            competition.getUpdateTime().format(DATE_FORMATTER) : null)
        .build();
  }

  private AdminCompetitionChallengeResponse toAdminCompetitionChallengeResponse(
      CompetitionChallenge relation, Competition competition) {
    Challenge challenge = challengeMapper.selectById(relation.getChallengeId());
    if (challenge == null) {
      return null;
    }

    // 统计本竞赛内解题数
    Integer solves;
    if (Boolean.TRUE.equals(competition.getIsTeamCompetition())) {
      solves = submissionMapper.countCompetitionChallengeSolvesByTeam(
          competition.getId(), challenge.getId());
    } else {
      solves = submissionMapper.countCompetitionChallengeSolvesByUser(
          competition.getId(), challenge.getId());
    }

    return AdminCompetitionChallengeResponse.builder()
        .id(relation.getId())
        .challengeId(challenge.getId())
        .title(challenge.getTitle())
        .category(challenge.getCategory())
        .difficulty(challenge.getDifficulty())
        .points(challenge.getPoints())
        .sortOrder(relation.getSortOrder())
        .solves(solves != null ? solves : 0)
        .build();
  }
}
