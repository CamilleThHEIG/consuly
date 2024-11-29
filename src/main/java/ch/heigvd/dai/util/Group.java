package ch.heigvd.dai.util;

public class Group {
    private String name;
    private int id_owner;
    private int[] id_members;

    public Group(String name, int id_owner, int[] id_members) {
        this.name = name;
        this.id_owner = id_owner;
        this.id_members = id_members;
    }

    public String name() { return name; }

    public void createGroup() {

    }

    public void deleteGroup() {

    }

    public void joinGroup() {

    }

    @Override
    public String toString() {
        return '[' + this.id_owner + ']' + this.name;
    }
}
