package main;

import utils.MyDatabase;

public class TestDB {
    public static void main(String[] args) {
        MyDatabase.getInstance().getConnection();
    }
}
