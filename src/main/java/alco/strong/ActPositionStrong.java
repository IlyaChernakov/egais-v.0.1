package alco.strong;

public class ActPositionStrong {

    private int number;
    private String quantity;
    private String fb;
    private String code;

    public ActPositionStrong(int number, String quantity, String fb, String code) {
        this.number = number;
        this.quantity = quantity;
        this.fb = fb;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}