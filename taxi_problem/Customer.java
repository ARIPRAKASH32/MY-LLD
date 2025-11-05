
public class Customer {

  private int customerId ;
  private char pickup;
  private char drop;
  private int pickTime;

  Customer(int id, char pickup, char drop, int pTime){
      this.customerId = id;
      this.pickup = pickup;
      this.drop = drop;
      this.pickTime = pTime;
  }

  public int getCustomerId(){
      return customerId;
  }

  public char getPickup(){
      return pickup;
  }

  public char getDrop(){
      return drop;
  }
  public int getPickTime(){
      return pickTime;
  }

}

