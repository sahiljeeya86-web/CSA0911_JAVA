abstract class Vehicle {
    protected final String regNo;
    protected Vehicle(String regNo) {
        this.regNo = regNo;
        System.out.println("  1. Vehicle(String) runs, regNo = " + regNo);
    }
}

class Car extends Vehicle {
    protected final int seats;
    Car(String regNo, int seats) {
        super(regNo);
        this.seats = seats;
        System.out.println("  2. Car(String, int) runs, seats = " + seats);
    }
    Car(String regNo) { this(regNo, 5); System.out.println("  (Car(String) runs and chains to Car(String, int))"); }
}

interface Chargeable { double CHARGE_RATE_PER_KWH = 12.0; }

class ElectricCar extends Car implements Chargeable {
    ElectricCar(String regNo) {
        super(regNo);
        System.out.println("  3. ElectricCar(String) runs");
    }
}

public class CtorTrace {
    public static void main(String[] args) {
        System.out.println("new ElectricCar(\"TN03C3\"):");
        new ElectricCar("TN03C3");
    }
}
