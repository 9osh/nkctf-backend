package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排行榜响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

  /**
   * 排行榜条目列表
   */
  private List<LeaderboardEntryResponse> entries;

  /**
   * 总用户数
   */
  private Long total;

  /**
   * 当前页码
   */
  private Integer page;

  /**
   * 总页数
   */
  private Integer pages;

  /**
   * 是否有下一页
   */
  private Boolean hasNext;

  /**
   * 是否有上一页
   */
  private Boolean hasPrevious;

  /**
   * 当前用户的排名信息（未登录时为 null）
   */
  private LeaderboardEntryResponse currentUserRank;
}
