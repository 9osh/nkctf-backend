package cn.edu.ndky.nkctf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

  /**
   * 当前页数据
   */
  private List<T> records;

  /**
   * 总记录数
   */
  private Long total;

  /**
   * 当前页码
   */
  private Integer page;

  /**
   * 每页大小
   */
  private Integer size;

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
}
