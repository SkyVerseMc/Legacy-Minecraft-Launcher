package net.minecraft.hopper;

public class PublishRequest {
  private String token;
  
  private int report_id;
  
  public PublishRequest(Report report) {
    this.report_id = report.getId();
    this.token = report.getToken();
  }
}


/* Location:              C:\Users\Admin\dev\LegacyLauncher\launcher\minecraft.jar!\net\minecraft\hopper\PublishRequest.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */