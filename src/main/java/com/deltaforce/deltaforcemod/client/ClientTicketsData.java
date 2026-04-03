package com.deltaforce.deltaforcemod.client;

public class ClientTicketsData {
    private static int currentTickets = 120;

    public static void setTickets(int tickets) {
        currentTickets = tickets;
    }

    public static int getTickets() {
        return currentTickets;
    }
}