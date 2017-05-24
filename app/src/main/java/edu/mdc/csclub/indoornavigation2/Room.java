package edu.mdc.csclub.indoornavigation2;

import android.media.Image;

/**
 * Created by transflorida on 5/23/17.
 */

public class Room {
    private int roomID;
    private String roomNumber;
    private String description;
    private String occupiedBy;
    private String roomPicture;


    public Room() {
    }

    public Room(int roomID, String roomNumber, String description, String occupiedBy, String roomPicture) {
        this.roomID = roomID;
        this.roomNumber = roomNumber;
        this.description = description;
        this.occupiedBy = occupiedBy;
        this.roomPicture = roomPicture;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOccupiedBy() {
        return occupiedBy;
    }

    public void setOccupiedBy(String occupiedBy) {
        this.occupiedBy = occupiedBy;
    }

    public String getRoomPicture() {
        return roomPicture;
    }

    public void setRoomPicture(String roomPicture) {
        this.roomPicture = roomPicture;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomID=" + roomID +
                ", roomNumber='" + roomNumber + '\'' +
                ", description='" + description + '\'' +
                ", occupiedBy='" + occupiedBy + '\'' +
                ", roomPicture=" + roomPicture +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        if (roomID != room.roomID) return false;
        return roomNumber != null ? roomNumber.equals(room.roomNumber) : room.roomNumber == null;

    }

    @Override
    public int hashCode() {
        int result = roomID;
        result = 31 * result + (roomNumber != null ? roomNumber.hashCode() : 0);
        return result;
    }
}
