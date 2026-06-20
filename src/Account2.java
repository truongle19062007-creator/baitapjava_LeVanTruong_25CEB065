public class Account2 {
    private int id;
    private Customer2 customer;
    private double balance =0.0;

    public Account2(int id, Customer2 customer, double balance){
        this.id = id;
        this.customer = customer;
        this.balance = balance;
    }
    public Account2(int id, Customer2 customer){
        this.id = id;
        this.customer = customer;
    }
    public int getId() {
        return id;
    }
    public Customer2 getCustomer() {
        return customer;
    }
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }
    public String toString(){
        return "name" + "(" + id + ")" + "balance" + "(" + balance + ")";
    }
    public String getCustomer2Name(){
        return customer.getName();
    }
    public Account2 deposit(double amount){
        this.balance += amount;
        return this;
    }
    public Account2 withdraw(double amount){
        if(this.balance >= amount){
            this.balance -= amount;
        }else {
            System.out.println("amount withdrawn exceeds the current balance");
        }
        return this;
    }
}
