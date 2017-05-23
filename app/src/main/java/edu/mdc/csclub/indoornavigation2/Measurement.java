package edu.mdc.csclub.indoornavigation2;

/**
 * Created by transflorida on 5/23/17.
 */

public class Measurement {
    private int X;
    private int Y;
    private int beacon11RSSI;
    private int beacon12RSSI;
    private int beacon21RSSI;
    private int beacon22RSSI;
    private int beacon31RSSI;
    private int beacon32RSSI;

    public Measurement() {
    }

    public Measurement(int x, int y, int beacon11RSSI, int beacon12RSSI, int beacon21RSSI, int beacon22RSSI, int beacon31RSSI, int beacon32RSSI) {
        X = x;
        Y = y;
        this.beacon11RSSI = beacon11RSSI;
        this.beacon12RSSI = beacon12RSSI;
        this.beacon21RSSI = beacon21RSSI;
        this.beacon22RSSI = beacon22RSSI;
        this.beacon31RSSI = beacon31RSSI;
        this.beacon32RSSI = beacon32RSSI;
    }

    public int getX() {
        return X;
    }

    public void setX(int x) {
        X = x;
    }

    public int getY() {
        return Y;
    }

    public void setY(int y) {
        Y = y;
    }

    public int getBeacon11RSSI() {
        return beacon11RSSI;
    }

    public void setBeacon11RSSI(int beacon11RSSI) {
        this.beacon11RSSI = beacon11RSSI;
    }

    public int getBeacon12RSSI() {
        return beacon12RSSI;
    }

    public void setBeacon12RSSI(int beacon12RSSI) {
        this.beacon12RSSI = beacon12RSSI;
    }

    public int getBeacon21RSSI() {
        return beacon21RSSI;
    }

    public void setBeacon21RSSI(int beacon21RSSI) {
        this.beacon21RSSI = beacon21RSSI;
    }

    public int getBeacon22RSSI() {
        return beacon22RSSI;
    }

    public void setBeacon22RSSI(int beacon22RSSI) {
        this.beacon22RSSI = beacon22RSSI;
    }

    public int getBeacon31RSSI() {
        return beacon31RSSI;
    }

    public void setBeacon31RSSI(int beacon31RSSI) {
        this.beacon31RSSI = beacon31RSSI;
    }

    public int getBeacon32RSSI() {
        return beacon32RSSI;
    }

    public void setBeacon32RSSI(int beacon32RSSI) {
        this.beacon32RSSI = beacon32RSSI;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "X=" + X +
                ", Y=" + Y +
                ", beacon11RSSI=" + beacon11RSSI +
                ", beacon12RSSI=" + beacon12RSSI +
                ", beacon21RSSI=" + beacon21RSSI +
                ", beacon22RSSI=" + beacon22RSSI +
                ", beacon31RSSI=" + beacon31RSSI +
                ", beacon32RSSI=" + beacon32RSSI +
                '}';
    }
}
