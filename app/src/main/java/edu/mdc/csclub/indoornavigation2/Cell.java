package edu.mdc.csclub.indoornavigation2;

/**
 * Created by transflorida on 5/23/17.
 */

public class Cell {
    private int x;
    private int y;
    private int roomID;


    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        roomID = 0;
    }

    public Cell(int x, int y, int roomID) {
        this.x = x;
        this.y = y;
        this.roomID = roomID;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    @Override
    public String toString() {
        return "Cell{" +
                "x=" + x +
                ", y=" + y +
                ", roomID='" + roomID + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cell cell = (Cell) o;

        if (x != cell.x) return false;
        if (y != cell.y) return false;
        return roomID == cell.roomID;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + roomID;
        return result;
    }
}
