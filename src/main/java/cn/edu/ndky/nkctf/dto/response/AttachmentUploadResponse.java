package cn.edu.ndky.nkctf.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 附件上传响应 DTO
 */
@Data
@Builder
public class AttachmentUploadResponse {

  /**
   * 附件访问 URL
   */
  private String url;

  /**
   * 附件文件名
   */
  private String name;
}
