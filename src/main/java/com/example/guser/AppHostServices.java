package com.example.guser;

import javafx.application.HostServices;

public class AppHostServices {
    private static HostServices hostServices;
    public static void init(HostServices hs) { hostServices = hs; }
    public static HostServices get() { return hostServices; }
}
