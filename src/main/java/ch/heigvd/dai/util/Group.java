package ch.heigvd.dai.util;

import java.util.LinkedList;

public class Group {
    private String name;
    private int idOwner;
    private LinkedList<Integer> membersIdList;
    private String password;

    public Group(String name, int id_owner, String pwd){
        this.name = name;
        this.idOwner = id_owner;
        this.membersIdList = new LinkedList<>();
        this.password = pwd;
    }

    private String password() { return password; }

    public int idOwner() { return idOwner; }

    public LinkedList<Integer> membersIdList() { return membersIdList; }

    public String name() { return name; }

    public void joinGroup(Integer newMemberId) {
        membersIdList.add(newMemberId);
    }

    public void leaveGroup(Integer memberID) { membersIdList.remove(memberID); }

    public boolean isMember(Integer memberId) { return membersIdList.contains(memberId); }

    public boolean isOwner(Integer memberId) { return this.idOwner == memberId; }

    @Override
    public String toString() { return '[' + this.idOwner + ']' + this.name(); }
}
