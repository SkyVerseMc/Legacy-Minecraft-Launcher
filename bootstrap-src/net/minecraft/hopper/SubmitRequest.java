package net.minecraft.hopper;

import java.util.Map;

public class SubmitRequest {
  private String report;
  
  private String version;
  
  private String product;
  
  private Map<String, String> environment;
  
  public SubmitRequest(String report, String product, String version, Map<String, String> environment) {
    this.report = report;
    this.version = version;
    this.product = product;
    this.environment = environment;
  }
}


/* Location:              C:\Users\Admin\dev\LegacyLauncher\launcher\minecraft.jar!\net\minecraft\hopper\SubmitRequest.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */