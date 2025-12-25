package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.dto.request.AdminUserQueryRequest;
import cn.edu.ndky.nkctf.dto.request.ChangeRoleRequest;
import cn.edu.ndky.nkctf.dto.request.ToggleEnabledRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateUserByAdminRequest;
import cn.edu.ndky.nkctf.dto.response.AdminUserDetailResponse;
import cn.edu.ndky.nkctf.dto.response.AdminUserListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;
import cn.edu.ndky.nkctf.entity.Team;
import cn.edu.ndky.nkctf.entity.TeamMember;
import cn.edu.ndky.nkctf.entity.User;
import cn.edu.ndky.nkctf.exception.BusinessException;
import cn.edu.ndky.nkctf.mapper.TeamMapper;
import cn.edu.ndky.nkctf.mapper.TeamMemberMapper;
import cn.edu.ndky.nkctf.mapper.UserMapper;
import cn.edu.ndky.nkctf.service.AdminUserService;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

  private final UserMapper userMapper;
  private final TeamMapper teamMapper;
  private final TeamMemberMapper teamMemberMapper;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

  @Override
  public PageResponse<AdminUserListItemResponse> getUserList(AdminUserQueryRequest request) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

    // 角色过滤
    if (StringUtils.hasText(request.getRole())) {
      String role = request.getRole().toUpperCase();
      if (ALLOWED_ROLES.contains(role)) {
        wrapper.eq(User::getRole, role);
      }
    }

    // 启用状态过滤
    if (request.getEnabled() != null) {
      wrapper.eq(User::getEnabled, request.getEnabled());
    }

    // 关键词搜索（用户名或邮箱）
    if (StringUtils.hasText(request.getKeyword())) {
      String keyword = request.getKeyword();
      wrapper.and(w -> w
          .like(User::getUsername, keyword)
          .or()
          .like(User::getEmail, keyword)
      );
    }

    // 按创建时间降序
    wrapper.orderByDesc(User::getCreateTime);

    // 分页查询
    Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
    Page<User> result = userMapper.selectPage(page, wrapper);

    // 获取团队名称映射
    List<User> users = result.getRecords();
    List<AdminUserListItemResponse> items = users.stream()
        .map(this::toAdminUserListItemResponse)
        .collect(Collectors.toList());

    return PageResponse.<AdminUserListItemResponse>builder()
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
  public AdminUserDetailResponse getUserDetail(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }
    return toAdminUserDetailResponse(user);
  }

  @Override
  @Transactional
  public AdminUserDetailResponse updateUser(Long userId, UpdateUserByAdminRequest request) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }

    User currentAdmin = getCurrentUser();

    // 更新昵称
    if (StringUtils.hasText(request.getNickname())) {
      user.setNickname(request.getNickname());
    }

    // 更新个性签名
    if (request.getBio() != null) {
      user.setBio(request.getBio());
    }

    // 更新角色
    if (StringUtils.hasText(request.getRole())) {
      String newRole = request.getRole().toUpperCase();
      if (!ALLOWED_ROLES.contains(newRole)) {
        throw new BusinessException(400, "无效的角色: " + request.getRole());
      }
      // 不能降级自己
      if (currentAdmin.getId().equals(userId) && "USER".equals(newRole)) {
        throw new BusinessException(400, "不能降级自己的角色");
      }
      user.setRole(newRole);
    }

    // 更新启用状态
    if (request.getEnabled() != null) {
      // 不能禁用自己
      if (currentAdmin.getId().equals(userId) && !request.getEnabled()) {
        throw new BusinessException(400, "不能禁用自己的账号");
      }
      user.setEnabled(request.getEnabled());
    }

    userMapper.updateById(user);
    log.info("管理员 {} 更新了用户 {} 的信息", currentAdmin.getUsername(), user.getUsername());

    return toAdminUserDetailResponse(user);
  }

  @Override
  @Transactional
  public void deleteUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }

    User currentAdmin = getCurrentUser();

    // 不能删除自己
    if (currentAdmin.getId().equals(userId)) {
      throw new BusinessException(400, "不能删除自己的账号");
    }

    userMapper.deleteById(userId);
    log.info("管理员 {} 删除了用户 {}", currentAdmin.getUsername(), user.getUsername());
  }

  @Override
  @Transactional
  public void toggleEnabled(Long userId, ToggleEnabledRequest request) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }

    User currentAdmin = getCurrentUser();

    // 不能禁用自己
    if (currentAdmin.getId().equals(userId) && !request.getEnabled()) {
      throw new BusinessException(400, "不能禁用自己的账号");
    }

    user.setEnabled(request.getEnabled());
    userMapper.updateById(user);

    String action = request.getEnabled() ? "启用" : "禁用";
    log.info("管理员 {} {}了用户 {}", currentAdmin.getUsername(), action, user.getUsername());
  }

  @Override
  @Transactional
  public void changeRole(Long userId, ChangeRoleRequest request) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(404, "用户不存在");
    }

    String newRole = request.getRole().toUpperCase();
    if (!ALLOWED_ROLES.contains(newRole)) {
      throw new BusinessException(400, "无效的角色: " + request.getRole());
    }

    User currentAdmin = getCurrentUser();

    // 不能降级自己
    if (currentAdmin.getId().equals(userId) && "USER".equals(newRole)) {
      throw new BusinessException(400, "不能降级自己的角色");
    }

    user.setRole(newRole);
    userMapper.updateById(user);

    log.info("管理员 {} 将用户 {} 的角色更改为 {}",
        currentAdmin.getUsername(), user.getUsername(), newRole);
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

  private AdminUserListItemResponse toAdminUserListItemResponse(User user) {
    String teamName = null;
    if (user.getTeamId() != null) {
      Team team = teamMapper.selectById(user.getTeamId());
      if (team != null) {
        teamName = team.getName();
      }
    }

    return AdminUserListItemResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .role(user.getRole())
        .enabled(user.getEnabled())
        .score(user.getScore() != null ? user.getScore() : 0)
        .solvedCount(user.getSolvedCount() != null ? user.getSolvedCount() : 0)
        .teamName(teamName)
        .createTime(user.getCreateTime() != null ?
            user.getCreateTime().format(DATE_FORMATTER) : null)
        .lastSubmitTime(user.getLastSubmitTime() != null ?
            user.getLastSubmitTime().format(DATE_FORMATTER) : null)
        .build();
  }

  private AdminUserDetailResponse toAdminUserDetailResponse(User user) {
    AdminUserDetailResponse.TeamInfo teamInfo = null;
    if (user.getTeamId() != null) {
      Team team = teamMapper.selectById(user.getTeamId());
      if (team != null) {
        // 获取用户在团队中的角色
        TeamMember member = teamMemberMapper.selectOne(
            new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, user.getTeamId())
                .eq(TeamMember::getUserId, user.getId())
        );
        String teamRole = member != null ? member.getRole() : "MEMBER";

        teamInfo = AdminUserDetailResponse.TeamInfo.builder()
            .id(team.getId().toString())
            .name(team.getName())
            .role(teamRole)
            .build();
      }
    }

    return AdminUserDetailResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .bio(user.getBio())
        .role(user.getRole())
        .enabled(user.getEnabled())
        .score(user.getScore() != null ? user.getScore() : 0)
        .solvedCount(user.getSolvedCount() != null ? user.getSolvedCount() : 0)
        .team(teamInfo)
        .createTime(user.getCreateTime() != null ?
            user.getCreateTime().format(DATE_FORMATTER) : null)
        .updateTime(user.getUpdateTime() != null ?
            user.getUpdateTime().format(DATE_FORMATTER) : null)
        .lastSubmitTime(user.getLastSubmitTime() != null ?
            user.getLastSubmitTime().format(DATE_FORMATTER) : null)
        .build();
  }
}
