package com.aequalis.wifiscanner;

public class NetworkModel {
    String networkName;
    String networkMac;
    Connection connectionStatus;

    public NetworkModel() {
    }

    public NetworkModel(String networkName, String networkMac, Connection connectionStatus) {
        this.networkName = networkName;
        this.networkMac = networkMac;
        this.connectionStatus = connectionStatus;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkMac() {
        return networkMac;
    }

    public void setNetworkMac(String networkMac) {
        this.networkMac = networkMac;
    }

    public Connection getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Connection connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}
