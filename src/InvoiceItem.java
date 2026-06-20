public class InvoiceItem {
    private String id;
    private String desc;
    private int qty;
    private double Unitprice;

    public InvoiceItem(String id, String desc, int qty, double Unitprice) {
        this.id = id;
        this.desc = desc;
        this.qty = qty;
        this.Unitprice = Unitprice;
    }
    public String getId() {
        return id;
    }
    public String getDesc() {
        return desc;
    }
    public int getQty() {
        return qty;
    }
    public void setQty(int qty) {
        this.qty = qty;
    }
    public double getUnitprice() {
        return Unitprice;
    }
    public void setUnitprice(double Unitprice) {
        this.Unitprice = Unitprice;
    }
    public double getTotal() {
        return Unitprice * qty;
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id='" + id + '\'' +
                ", desc='" + desc + '\'' +
                ", qty=" + qty +
                ", Unitprice=" + Unitprice +
                '}';
    }
}
