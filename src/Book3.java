public class Book3 {
    private String Isbn;
    private String name;
    private Author2 author;
    private double price;
    private int qty = 0;

    public Book3(String Isbn, String name, Author2 author, double price) {
        this.Isbn = Isbn;
        this.name = name;
        this.author = author;
        this.price = price;
    }
    public Book3(String Isbn, String name, Author2 author, double price, int qty) {
        this.Isbn = Isbn;
        this.name = name;
        this.author = author;
        this.price = price;
        this.qty = qty;
    }
    public String getIsbn() {
        return Isbn;
    }
    public String getName() {
        return name;
    }
    public Author2 getAuthor() {
        return author;
    }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public int getQty() {
        return qty;
    }
    public void setQty(int qty) {
        this.qty = qty;
    }
    public String getAuthorName() {
        return author.getName();
    }

    @Override
    public String toString() {
        return "Book3{" +
                "isbn='" + Isbn + '\'' +
                ", name='" + name + '\'' +
                ", author=" + author +
                ", price=" + price +
                ", qty=" + qty +
                '}';
    }
}
