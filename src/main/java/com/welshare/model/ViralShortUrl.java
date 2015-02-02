package com.welshare.model;

public class ViralShortUrl extends ShortUrl {

    private int nodesFromBeginning;
    private int averageSubgraphDepth;
    private int viralPoints;
    private double viralPointsPercentage;

    public int getNodesFromBeginning() {
        return nodesFromBeginning;
    }
    public void setNodesFromBeginning(int nodesFromBeginning) {
        this.nodesFromBeginning = nodesFromBeginning;
    }
    public int getAverageSubgraphDepth() {
        return averageSubgraphDepth;
    }
    public void setAverageSubgraphDepth(int totalLevels) {
        this.averageSubgraphDepth = totalLevels;
    }
    public int getViralPoints() {
        return viralPoints;
    }
    public void setViralPoints(int viralPoints) {
        this.viralPoints = viralPoints;
    }

    public double getViralPointsPercentage() {
        return viralPointsPercentage;
    }
    public void setViralPointsPercentage(double viralPointsPercentage) {
        this.viralPointsPercentage = viralPointsPercentage;
    }
    @Override
    public String toString() {
        return "ViralShortUrl [nodesFromBeginning=" + nodesFromBeginning
                + ", averageSubgraphDepth=" + averageSubgraphDepth
                + ", viralPoints=" + viralPoints + ", viralPointsPercentage="
                + viralPointsPercentage + ", getKey()=" + getKey()
                + ", getLongUrl()=" + getLongUrl() + ", getUser()=" + getUser()
                + "]";
    }

}
