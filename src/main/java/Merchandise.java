import java.util.Objects;

public class Merchandise {

    String seller;
    String market;
    long price;
    String name;

    public Merchandise(String mar, String sel, long pri, String nam) {
        seller=sel;
        market=mar;
        price=pri;
        name = nam;
    }

    public long getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchandise that = (Merchandise) o;
        return price == that.price &&
                Objects.equals(seller, that.seller) &&
                Objects.equals(market, that.market) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seller, market, price, name);
    }
}
