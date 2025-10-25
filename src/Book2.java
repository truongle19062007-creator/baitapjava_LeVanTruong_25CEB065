import java.util.Arrays;
import java.util.stream.Collectors;

public class Book2 {
    private String name;
    private Author[] author;
    private double price;
    private int qty = 0;

    public Book2(String name, Author[] author, double price) {
        this.name = name;
        this.author = author;
        this.price = price;
    }
    public Book2(String name, Author[] author, double price, int qty) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.qty = qty;
    }
    public String getName() {
        return name;
    }
    public Author[] getAuthor() {
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

    @Override
    public String toString() {
        return "Book[" +
                "name='" + name + '\'' +
                ", author=" + Arrays.toString(author) +
                ", price=" + price +
                ", qty=" + qty +
                '}';
    }
    public String getAuthorNamess(){
        String listName = "";
        for (Author a : author) {
            listName = listName + a.getName() ;
        }
        return listName;
    }
}
