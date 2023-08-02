package net.minecraft.hopper;

public class PublishResponse extends Response {
  private Report report;
  
  private Crash crash;
  
  private Problem problem;
  
  public Report getReport() {
    return this.report;
  }
  
  public Crash getCrash() {
    return this.crash;
  }
  
  public Problem getProblem() {
    return this.problem;
  }
}


/* Location:              C:\Users\Admin\dev\LegacyLauncher\launcher\minecraft.jar!\net\minecraft\hopper\PublishResponse.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */