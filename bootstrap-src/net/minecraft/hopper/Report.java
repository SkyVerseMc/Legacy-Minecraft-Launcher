package net.minecraft.hopper;

public class Report {
  private int id;
  
  private boolean published;
  
  private String token;
  
  public int getId() {
    return this.id;
  }
  
  public boolean isPublished() {
    return this.published;
  }
  
  public String getToken() {
    return this.token;
  }
  
  public boolean canBePublished() {
    return (getToken() != null);
  }
}


/* Location:              C:\Users\Admin\dev\LegacyLauncher\launcher\minecraft.jar!\net\minecraft\hopper\Report.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */