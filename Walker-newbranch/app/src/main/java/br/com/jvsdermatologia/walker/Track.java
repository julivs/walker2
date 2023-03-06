package br.com.jvsdermatologia.walker;

import java.util.List;

public class Track {
    private double distance;
    private double time;
    private long end;
    private List<Double[]> points;

    public Track(long end, double distance, double time, List<Double[]> points) {
        this.distance = distance;
        this.time = time;
        this.points = points;
        this.end = end;
    }

    public Track() {
    }

    public double getDistance() {
        return distance;
    }

    public double getTime() {
        return time;
    }

    public long getEnd() {
        return end;
    }

    public List<Double[]> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "Track{" +
                "end=" + end +
                ", distance=" + distance +
                ", time=" + time +
                ", points=" + listList() +
                '}';
    }

    private String listList(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (Double[] doubles : points) {
            stringBuilder.append("[").append(doubles[0]).append(",").append(doubles[1]).append("],");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
