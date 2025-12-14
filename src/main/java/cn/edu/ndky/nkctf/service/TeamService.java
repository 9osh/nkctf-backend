package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.CreateTeamRequest;
import cn.edu.ndky.nkctf.dto.request.JoinTeamRequest;
import cn.edu.ndky.nkctf.dto.request.TransferCaptainRequest;
import cn.edu.ndky.nkctf.dto.response.InviteTokenResponse;
import cn.edu.ndky.nkctf.dto.response.RefreshTokenResponse;
import cn.edu.ndky.nkctf.dto.response.TeamResponse;

/**
 * 队伍服务接口
 *
 * <h2>安全设计原则</h2>
 * <ul>
 *   <li>所有操作的用户身份从 JWT Token 中的 SecurityContext 获取，绝不信任请求参数</li>
 *   <li>队长权限操作会在服务端严格验证，通过比对 JWT 中的用户 ID 与数据库中的 captainId</li>
 *   <li>敏感信息（如邀请令牌）通过专用接口提供，仅队长可访问</li>
 * </ul>
 */
public interface TeamService {

  /**
   * 创建队伍
   * @param request 创建请求
   * @return 队伍信息
   */
  TeamResponse createTeam(CreateTeamRequest request);

  /**
   * 获取队伍详情
   * @param teamId 队伍 ID
   * @return 队伍信息
   */
  TeamResponse getTeam(String teamId);

  /**
   * 获取当前用户的队伍
   * @return 队伍信息
   */
  TeamResponse getMyTeam();

  /**
   * 加入队伍
   * @param request 加入请求
   * @return 队伍信息
   */
  TeamResponse joinTeam(JoinTeamRequest request);

  /**
   * 退出队伍
   */
  void leaveTeam();

  /**
   * 获取邀请令牌（仅队长）
   *
   * <h3>安全说明</h3>
   * <ul>
   *   <li>从 JWT Token 中获取当前用户 ID</li>
   *   <li>验证当前用户是队伍队长</li>
   *   <li>仅返回给队长，普通成员调用会返回 403</li>
   * </ul>
   *
   * @return 邀请令牌响应
   */
  InviteTokenResponse getInviteToken();

  /**
   * 刷新邀请令牌（仅队长）
   * @return 刷新结果
   */
  RefreshTokenResponse refreshInviteToken();

  /**
   * 移除队伍成员（仅队长）
   *
   * <h3>安全说明</h3>
   * <ul>
   *   <li>从 JWT Token 中获取当前用户 ID，验证是否为队长</li>
   *   <li>targetUserId 需要验证是当前队伍的成员</li>
   *   <li>队长不能移除自己</li>
   * </ul>
   *
   * @param targetUserId 要移除的成员用户 ID
   */
  void removeMember(Long targetUserId);

  /**
   * 转让队长（仅队长）
   *
   * <h3>安全说明</h3>
   * <ul>
   *   <li>从 JWT Token 中获取当前用户 ID，验证是否为队长</li>
   *   <li>newCaptainId 需要验证是当前队伍的成员</li>
   *   <li>操作完成后原队长变为普通成员</li>
   * </ul>
   *
   * @param request 转让请求
   */
  void transferCaptain(TransferCaptainRequest request);

  /**
   * 解散队伍（仅队长）
   */
  void disbandTeam();
}
