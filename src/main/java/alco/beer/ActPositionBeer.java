package alco.beer;

public class ActPositionBeer {

    private int number;
    private String quantity;
    private String fb;

    public ActPositionBeer(int number, String quantity, String fb) {
        this.number = number;
        this.quantity = quantity;
        this.fb = fb;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getFb() {
        return fb;
    }

    public void setFb(String fb) {
        this.fb = fb;
    }
}
