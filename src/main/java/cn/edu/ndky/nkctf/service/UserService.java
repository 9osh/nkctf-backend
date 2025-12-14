package cn.edu.ndky.nkctf.service;

import cn.edu.ndky.nkctf.dto.response.UserProfileResponse;

/**
 * 用户服务接口
 */
public interface UserService {

  /**
   * 获取用户资料
   * @param userId 用户ID
   * @return 用户资料
   */
  UserProfileResponse getUserProfile(Long userId);

  /**
   * 获取当前登录用户资料
   * @return 用户资料
   */
  UserProfileResponse getCurrentUserProfile();

  /**
   * 更新当前用户的个性签名
   * @param bio 新的个性签名
   */
  void updateBio(String bio);
}
