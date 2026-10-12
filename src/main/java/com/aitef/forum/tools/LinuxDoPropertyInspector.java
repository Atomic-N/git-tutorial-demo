package com.aitef.forum.tools;

public final class LinuxDoPropertyInspector {
    private LinuxDoPropertyInspector() {
    }

    public static void main(String[] args) {
        System.out.println("linuxdo.cache.dir=" + System.getProperty("linuxdo.cache.dir"));
        System.out.println("linuxdo.cache.only=" + System.getProperty("linuxdo.cache.only"));
        System.out.println("linuxdo.import.limit=" + System.getProperty("linuxdo.import.limit"));
    }
}
