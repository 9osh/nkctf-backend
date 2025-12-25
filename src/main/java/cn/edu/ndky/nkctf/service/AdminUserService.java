package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.request.AdminUserQueryRequest;
import cn.edu.ndky.nkctf.dto.request.ChangeRoleRequest;
import cn.edu.ndky.nkctf.dto.request.ToggleEnabledRequest;
import cn.edu.ndky.nkctf.dto.request.UpdateUserByAdminRequest;
import cn.edu.ndky.nkctf.dto.response.AdminUserDetailResponse;
import cn.edu.ndky.nkctf.dto.response.AdminUserListItemResponse;
import cn.edu.ndky.nkctf.dto.response.PageResponse;

/**
 * 管理员用户管理服务接口
 */
public interface AdminUserService {

  /**
   * 获取用户列表（分页）
   */
  PageResponse<AdminUserListItemResponse> getUserList(AdminUserQueryRequest request);

  /**
   * 获取用户详情
   */
  AdminUserDetailResponse getUserDetail(Long userId);

  /**
   * 更新用户信息
   */
  AdminUserDetailResponse updateUser(Long userId, UpdateUserByAdminRequest request);

  /**
   * 删除用户（软删除）
   */
  void deleteUser(Long userId);

  /**
   * 切换用户启用状态
   */
  void toggleEnabled(Long userId, ToggleEnabledRequest request);

  /**
   * 更改用户角色
   */
  void changeRole(Long userId, ChangeRoleRequest request);
}
