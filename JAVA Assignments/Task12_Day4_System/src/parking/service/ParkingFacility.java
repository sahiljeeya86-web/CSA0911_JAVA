package parking.service;

/** Physical layout: jagged array, one row per floor, one cell per slot. */
public class ParkingFacility {
    private final String[][] slots;      // slots[floor][slot] = registration number or null

    public ParkingFacility(int[] slotsPerFloor) {
        if (slotsPerFloor == null || slotsPerFloor.length == 0) {
            throw new IllegalArgumentException("at least one floor is required");
        }
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            if (slotsPerFloor[f] <= 0) throw new IllegalArgumentException("each floor needs at least one slot");
            slots[f] = new String[slotsPerFloor[f]];
        }
    }

    public int[] allocate(String regNo) {
        for (int f = 0; f < slots.length; f++) {
            for (int s = 0; s < slots[f].length; s++) {
                if (slots[f][s] == null) {
                    slots[f][s] = regNo;
                    return new int[]{f, s};
                }
            }
        }
        return null;                     // facility is full
    }

    public boolean release(int floor, int slot) {
        if (!isValid(floor, slot) || slots[floor][slot] == null) return false;
        slots[floor][slot] = null;
        return true;
    }

    public boolean isValid(int floor, int slot) {
        return floor >= 0 && floor < slots.length && slot >= 0 && slot < slots[floor].length;
    }

    public int floors() { return slots.length; }

    public int totalSlots() {
        int n = 0;
        for (String[] row : slots) n += row.length;
        return n;
    }

    public int occupiedSlots() {
        int n = 0;
        for (String[] row : slots) for (String v : row) if (v != null) n++;
        return n;
    }
}
