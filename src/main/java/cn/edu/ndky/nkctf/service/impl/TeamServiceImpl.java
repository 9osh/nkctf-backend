package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.CreateTeamRequest;
import cn.edu.ndky.nkctf.dto.request.JoinTeamRequest;
import cn.edu.ndky.nkctf.dto.request.TransferCaptainRequest;
import cn.edu.ndky.nkctf.dto.response.InviteTokenResponse;
import cn.edu.ndky.nkctf.dto.response.RefreshTokenResponse;
import cn.edu.ndky.nkctf.dto.response.TeamResponse;
import cn.edu.ndky.nkctf.entity.Team;
import cn.edu.ndky.nkctf.entity.TeamMember;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.TeamMapper;
import cn.edu.ndky.nkctf.mapper.TeamMemberMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.TeamService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 队伍服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

  private final TeamMapper teamMapper;
  private final TeamMemberMapper teamMemberMapper;
  private final UserMapper userMapper;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  @Transactional
  public TeamResponse createTeam(CreateTeamRequest request) {
    User currentUser = getCurrentUser();

    // 检查用户是否已有队伍
    if (currentUser.getTeamId() != null) {
      throw new BusinessException(400, "您已经加入了一个队伍，请先退出当前队伍");
    }

    // 检查队伍名称是否已存在（仅检查未删除的队伍）
    Team existingTeam = teamMapper.selectOne(
        new LambdaQueryWrapper<Team>().eq(Team::getName, request.getName()));
    if (existingTeam != null) {
      throw new BusinessException(400, "队伍名称已存在");
    }

    // 物理删除已逻辑删除的同名队伍（释放队伍名称）
    teamMapper.physicalDeleteByName(request.getName());

    // 创建队伍
    Team team = new Team();
    team.setId(UUID.randomUUID());
    team.setName(request.getName());
    team.setDescription(request.getDescription());
    team.setInviteToken(UUID.randomUUID());
    team.setCaptainId(currentUser.getId());
    teamMapper.insert(team);

    // 创建队长成员记录
    TeamMember member = new TeamMember();
    member.setTeamId(team.getId());
    member.setUserId(currentUser.getId());
    member.setRole(TeamMember.Role.CAPTAIN.getValue());
    teamMemberMapper.insert(member);

    // 更新用户的队伍 ID
    currentUser.setTeamId(team.getId());
    userMapper.updateById(currentUser);

    log.info("用户 {} 创建了队伍 {}", currentUser.getUsername(), team.getName());

    return buildTeamResponse(team);
  }

  @Override
  public TeamResponse getTeam(String teamId) {
    // 解析队伍 ID
    UUID teamUuid;
    try {
      teamUuid = UUID.fromString(teamId);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(400, "队伍 ID 格式无效");
    }

    Team team = teamMapper.selectById(teamUuid);
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    return buildTeamResponse(team);
  }

  @Override
  public TeamResponse getMyTeam() {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(404, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    return buildTeamResponse(team);
  }

  @Override
  @Transactional
  public TeamResponse joinTeam(JoinTeamRequest request) {
    User currentUser = getCurrentUser();

    // 检查用户是否已有队伍
    if (currentUser.getTeamId() != null) {
      throw new BusinessException(400, "您已经加入了一个队伍，请先退出当前队伍");
    }

    // 解析邀请令牌
    UUID inviteToken;
    try {
      inviteToken = UUID.fromString(request.getInviteToken());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(400, "邀请码格式无效");
    }

    // 根据邀请令牌查找队伍
    Team team = teamMapper.selectOne(
        new LambdaQueryWrapper<Team>().eq(Team::getInviteToken, inviteToken));
    if (team == null) {
      throw new BusinessException(404, "邀请码无效或队伍不存在");
    }

    // 创建成员记录
    TeamMember member = new TeamMember();
    member.setTeamId(team.getId());
    member.setUserId(currentUser.getId());
    member.setRole(TeamMember.Role.MEMBER.getValue());
    teamMemberMapper.insert(member);

    // 更新用户的队伍 ID
    currentUser.setTeamId(team.getId());
    userMapper.updateById(currentUser);

    log.info("用户 {} 加入了队伍 {}", currentUser.getUsername(), team.getName());

    return buildTeamResponse(team);
  }

  @Override
  @Transactional
  public void leaveTeam() {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    // 队长不能退出，只能解散
    if (team.getCaptainId().equals(currentUser.getId())) {
      throw new BusinessException(400, "队长不能退出队伍，请先转让队长或解散队伍");
    }

    // 删除成员记录
    teamMemberMapper.delete(
        new LambdaQueryWrapper<TeamMember>()
            .eq(TeamMember::getTeamId, team.getId())
            .eq(TeamMember::getUserId, currentUser.getId()));

    // 更新用户的队伍 ID（使用 UpdateWrapper 显式设置 null）
    userMapper.update(
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
            .eq(User::getId, currentUser.getId())
            .set(User::getTeamId, null));

    log.info("用户 {} 退出了队伍 {}", currentUser.getUsername(), team.getName());
  }

  @Override
  @Transactional
  public RefreshTokenResponse refreshInviteToken() {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    // 验证是否为队长
    if (!team.getCaptainId().equals(currentUser.getId())) {
      throw new BusinessException(403, "只有队长才能刷新邀请令牌");
    }

    // 生成新的邀请令牌
    UUID newToken = UUID.randomUUID();
    team.setInviteToken(newToken);
    teamMapper.updateById(team);

    log.info("队伍 {} 的邀请令牌已刷新", team.getName());

    return RefreshTokenResponse.builder()
        .teamId(team.getId().toString())
        .newInviteToken(newToken.toString())
        .message("邀请令牌已刷新")
        .build();
  }

  @Override
  @Transactional
  public void disbandTeam() {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    // 验证是否为队长
    if (!team.getCaptainId().equals(currentUser.getId())) {
      throw new BusinessException(403, "只有队长才能解散队伍");
    }

    // 获取所有成员
    List<Long> memberIds = teamMemberMapper.getTeamMemberIds(team.getId());

    // 清空所有成员的队伍 ID（使用 UpdateWrapper 显式设置 null）
    for (Long memberId : memberIds) {
      userMapper.update(
          new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
              .eq(User::getId, memberId)
              .set(User::getTeamId, null));
    }

    // 删除所有成员记录
    teamMemberMapper.delete(
        new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, team.getId()));

    // 删除队伍（逻辑删除，使用 UpdateWrapper 显式设置）
    teamMapper.update(
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Team>()
            .eq(Team::getId, team.getId())
            .set(Team::getDeleted, 1));

    log.info("队伍 {} 已被队长 {} 解散", team.getName(), currentUser.getUsername());
  }

  @Override
  public InviteTokenResponse getInviteToken() {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    /*
     * 安全验证：队长权限检查
     *
     * 验证逻辑：
     * 1. currentUser.getId() 来自 JWT Token 中的用户名，通过数据库查询获取用户 ID
     * 2. team.getCaptainId() 来自数据库中的队伍记录
     * 3. 两者比对确保当前用户确实是队长，无法通过篡改请求参数绕过
     */
    if (!team.getCaptainId().equals(currentUser.getId())) {
      log.warn("安全警告: 用户 {} (ID:{}) 尝试获取队伍 {} 的邀请令牌，但不是队长",
          currentUser.getUsername(), currentUser.getId(), team.getName());
      throw new BusinessException(403, "只有队长才能查看邀请令牌");
    }

    return InviteTokenResponse.builder()
        .teamId(team.getId().toString())
        .teamName(team.getName())
        .inviteToken(team.getInviteToken().toString())
        .build();
  }

  @Override
  @Transactional
  public void removeMember(Long targetUserId) {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    /*
     * 安全验证：队长权限检查
     *
     * 验证逻辑：
     * 1. currentUser 从 JWT Token 中获取，不可伪造
     * 2. 比对 currentUser.getId() 与 team.getCaptainId()
     * 3. 黑客无法通过修改请求参数（如 URL 中的 userId）来冒充队长操作
     */
    if (!team.getCaptainId().equals(currentUser.getId())) {
      log.warn("安全警告: 用户 {} (ID:{}) 尝试从队伍 {} 移除成员，但不是队长",
          currentUser.getUsername(), currentUser.getId(), team.getName());
      throw new BusinessException(403, "只有队长才能移除成员");
    }

    // 队长不能移除自己
    if (targetUserId.equals(currentUser.getId())) {
      throw new BusinessException(400, "队长不能移除自己，请使用解散队伍或转让队长功能");
    }

    // 验证目标用户是否为队伍成员
    User targetUser = userMapper.selectById(targetUserId);
    if (targetUser == null) {
      throw new BusinessException(404, "目标用户不存在");
    }

    if (!team.getId().equals(targetUser.getTeamId())) {
      throw new BusinessException(400, "该用户不是您队伍的成员");
    }

    // 删除成员记录
    teamMemberMapper.delete(
        new LambdaQueryWrapper<TeamMember>()
            .eq(TeamMember::getTeamId, team.getId())
            .eq(TeamMember::getUserId, targetUserId));

    // 更新目标用户的队伍 ID（使用 UpdateWrapper 显式设置 null）
    userMapper.update(
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
            .eq(User::getId, targetUserId)
            .set(User::getTeamId, null));

    log.info("队长 {} 将成员 {} 移出了队伍 {}",
        currentUser.getUsername(), targetUser.getUsername(), team.getName());
  }

  @Override
  @Transactional
  public void transferCaptain(TransferCaptainRequest request) {
    User currentUser = getCurrentUser();

    if (currentUser.getTeamId() == null) {
      throw new BusinessException(400, "您尚未加入任何队伍");
    }

    Team team = teamMapper.selectById(currentUser.getTeamId());
    if (team == null) {
      throw new BusinessException(404, "队伍不存在");
    }

    /*
     * 安全验证：队长权限检查
     *
     * 验证逻辑：
     * 1. currentUser 从 JWT Token 中获取，身份可信
     * 2. 只有数据库中记录的队长才能执行转让操作
     * 3. request.getNewCaptainId() 会进一步验证是否为队伍成员
     */
    if (!team.getCaptainId().equals(currentUser.getId())) {
      log.warn("安全警告: 用户 {} (ID:{}) 尝试转让队伍 {} 的队长权限，但不是队长",
          currentUser.getUsername(), currentUser.getId(), team.getName());
      throw new BusinessException(403, "只有队长才能转让队长权限");
    }

    Long newCaptainId = request.getNewCaptainId();

    // 不能转让给自己
    if (newCaptainId.equals(currentUser.getId())) {
      throw new BusinessException(400, "您已经是队长");
    }

    // 验证新队长是否为队伍成员
    User newCaptain = userMapper.selectById(newCaptainId);
    if (newCaptain == null) {
      throw new BusinessException(404, "目标用户不存在");
    }

    if (!team.getId().equals(newCaptain.getTeamId())) {
      throw new BusinessException(400, "该用户不是您队伍的成员");
    }

    // 更新队伍的队长 ID
    team.setCaptainId(newCaptainId);
    teamMapper.updateById(team);

    // 更新成员角色：原队长变成员
    TeamMember oldCaptainMember = teamMemberMapper.selectOne(
        new LambdaQueryWrapper<TeamMember>()
            .eq(TeamMember::getTeamId, team.getId())
            .eq(TeamMember::getUserId, currentUser.getId()));
    if (oldCaptainMember != null) {
      oldCaptainMember.setRole(TeamMember.Role.MEMBER.getValue());
      teamMemberMapper.updateById(oldCaptainMember);
    }

    // 更新成员角色：新队长
    TeamMember newCaptainMember = teamMemberMapper.selectOne(
        new LambdaQueryWrapper<TeamMember>()
            .eq(TeamMember::getTeamId, team.getId())
            .eq(TeamMember::getUserId, newCaptainId));
    if (newCaptainMember != null) {
      newCaptainMember.setRole(TeamMember.Role.CAPTAIN.getValue());
      teamMemberMapper.updateById(newCaptainMember);
    }

    log.info("队伍 {} 的队长由 {} 转让给 {}",
        team.getName(), currentUser.getUsername(), newCaptain.getUsername());
  }

  /**
   * 构建队伍响应
   *
   * <p>安全说明：此方法不返回敏感信息（如邀请令牌）。
   * 邀请令牌通过专用接口 getInviteToken() 提供，仅队长可访问。</p>
   */
  private TeamResponse buildTeamResponse(Team team) {
    // 获取队伍成员
    List<Long> memberIds = teamMemberMapper.getTeamMemberIds(team.getId());
    List<User> members = memberIds.stream()
        .map(userMapper::selectById)
        .filter(u -> u != null)
        .collect(Collectors.toList());

    // 构建成员列表
    List<TeamResponse.TeamMember> memberResponses = members.stream()
        .map(u -> {
          String role = u.getId().equals(team.getCaptainId())
              ? TeamMember.Role.CAPTAIN.getValue()
              : TeamMember.Role.MEMBER.getValue();
          return TeamResponse.TeamMember.builder()
              .id(u.getId())
              .username(u.getUsername())
              .nickname(u.getNickname())
              .avatar(u.getAvatar())
              .role(role)
              .build();
        })
        .collect(Collectors.toList());

    // 获取队长信息
    TeamResponse.TeamMember captain = memberResponses.stream()
        .filter(m -> m.getId().equals(team.getCaptainId()))
        .findFirst()
        .orElse(null);

    return TeamResponse.builder()
        .id(team.getId().toString())
        .name(team.getName())
        .description(team.getDescription())
        .captain(captain)
        .members(memberResponses)
        .memberCount(members.size())
        .createTime(team.getCreateTime() != null
            ? team.getCreateTime().format(DATE_FORMATTER)
            : null)
        .build();
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
        new LambdaQueryWrapper<User>().eq(User::getUsername, userDetails.getUsername()));

    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    return user;
  }
}
